/**
 *
 * @author Ruslan Kutugin
 */
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

public class JavaLife {//запускающий класс
    public static void main(String[] arguments) {
        ThreadPool pool = new ThreadPool(2);//1 - game;2 - music
        pool.runTask(new Visual());
        pool.runTask(new Sounds());
        pool.join();
    }
}
//Класс, управляющий мультипоточностью, для музыки и игры одновременно
class ThreadPool extends ThreadGroup {

    private static IDAssigner poolID = new IDAssigner(1);
    private boolean alive;
    private List<Runnable> taskQueue;
    private  int id;

    public ThreadPool(int numThreads) {
        super("ThreadPool");
        this.id = poolID.getCurrentID();
        setDaemon(true);//этот поток закроется когда закроется main поток
        taskQueue = new LinkedList<Runnable>();
        alive = true;
        for(int i =0;i<numThreads;i++){
            new PooledThread(this).start();
        }
    }

    public synchronized void runTask(Runnable task) {
        if(!alive) throw new IllegalStateException("ThreaadPool is dead");
        if(task!=null){
            taskQueue.add(task);
            notify();
        }
    }

    public synchronized void close() {
        if(!alive) return;
        alive = false;
        taskQueue.clear();
        interrupt();
    }

    public  void join(){
        synchronized(this) {
            alive = false;
            notifyAll();
        }
        Thread[] threads = new Thread[activeCount()];
        int count = enumerate(threads);
        for(int i = 0; i<count;i++){
            try{
                threads[i].join();
            } catch (InterruptedException e) {e.printStackTrace();}
        }
    }

    protected synchronized Runnable getTask() throws InterruptedException {
        while (taskQueue.size()==0){
            if(!alive) return null;
            wait();
        }
        return taskQueue.remove(0);
    }

}

class PooledThread extends Thread {
    private static  IDAssigner threadID = new IDAssigner(1);
    private ThreadPool pool;

    public PooledThread(ThreadPool pool) {
        super(pool,"PooledThread-" + threadID.next());
        this.pool = pool;
    }
    @Override

    public  void run() {
        while (!isInterrupted()){
            Runnable task = null;
            try {
                task = pool.getTask();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(task == null) return;
            try {
                task.run();
            } catch(Throwable t) {
                pool.uncaughtException(this,t);
            }
        }
    }
}

class IDAssigner {
    private int baseID;
    public IDAssigner(int baseID){
        this.baseID = baseID;
    }
    public int next() {
        return baseID++;
    }
    public  int getCurrentID(){
        return baseID;
    }
}

//Игровой блок
class Visual implements Runnable {
    @Override
    public void run() {
        new Filling();
    }
}
//Класс, управляющий наполнением
class Filling extends JFrame implements ActionListener {
    public static String mode ="Menu";//Режим при входе в игру
    public static volatile boolean audioClick =false;
    public static volatile boolean audioGame=false;
    public static volatile boolean audioMenu=true;
    public static volatile boolean audioEnabled=false;
    public static volatile boolean border=false;
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);
    private ButtonMenu menu = new ButtonMenu();
    private ControlPanel controlPanel = new ControlPanel();//создаю экземпляр интерфейса управления
    private JPanel global = new JPanel();//создаю панель игры
    private Settings set = new Settings();     //экз настройки
    private About abo = new About();    //экз про
    private Quit ex = new Quit();    //экз выход
    private Timer mainTimer = new Timer(30,this);
    public  static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public static Color color = new Color(244, 205, 138);
    public Filling() {
        super("JavaLife");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/icon.png")));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setExtendedState(MAXIMIZED_BOTH);
        global.setLayout(new GridLayout(1,2));
        global.add(controlPanel.createField.p);//добавляю поле в панель
        global.add(controlPanel.tabbedPane);//добавляю интерфейс управления в панель
        menu.panel.setBackground(new Color(244, 205, 138));
        controlPanel.p.setBackground(new Color(244, 205, 138));
        controlPanel.createField.p.setBackground(new Color(244, 205, 138));
        set.panel.setBackground(color);
        abo.panel.setBackground(color);
        ex.panel.setBackground(color);
        global.setLocation(0, 0);
        global.setSize(screenSize);
        cardPanel.add(menu.panel,"Menu");
        cardPanel.add(global,"Game");//игрa
        cardPanel.add(set.panel,"Settings");//настройки
        cardPanel.add(abo.panel,"About");//про
        cardPanel.add(ex.panel,"Quit");//выход*/
        cardLayout.show(cardPanel, mode);//!
        add(cardPanel);
        mainTimer.start();
    }

    public void actionPerformed(ActionEvent e) {
        cardLayout.show(cardPanel, mode);
        if(SuperButton.thorFlag) set.thorStateLabel.setText("Thor mode is ON");
        else set.thorStateLabel.setText("Thor mode is OFF");
        if(Filling.audioEnabled) set.soundStateLabel.setText("Sound is ON");
        else set.soundStateLabel.setText("Sound is OFF");
    }

    public static ImageIcon createImageIcon(String path) {//возвращает ImageIcon или null если путь неверен
        java.net.URL imgURL = Filling.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
//Меню
class ButtonMenu extends JFrame {
    JPanel panel = new JPanel();
    private SuperButton b1 = new SuperButton("Game");
    private SuperButton b2 = new SuperButton("Settings");
    private SuperButton b3 = new SuperButton("About");
    private SuperButton b4 = new SuperButton("Quit");

    public ButtonMenu() {
        panel.setLayout(null);
        panel.setSize(Filling.screenSize);
        Dimension size = b1.getSize();
        b1.setBounds((int) (panel.getWidth()/2)-(b1.getWidth()/2), (int)((panel.getHeight())/5)*1-(b1.getHeight()), size.width, size.height);
        b2.setBounds((int) (panel.getWidth()/2)-(b1.getWidth()/2), (int)((panel.getHeight())/5)*2-(b1.getHeight()), size.width, size.height);
        b3.setBounds((int) (panel.getWidth()/2)-(b1.getWidth()/2), (int)((panel.getHeight())/5)*3-(b1.getHeight()), size.width, size.height);
        b4.setBounds((int) (panel.getWidth()/2)-(b1.getWidth()/2), (int)((panel.getHeight())/5)*4-(b1.getHeight()), size.width, size.height);
        panel.add(b1);
        panel.add(b2);
        panel.add(b3);
        panel.add(b4);
    }
}
//инкапсулировать дальше!
class SuperButton extends JButton implements ActionListener { //кнопка меню

    ImageIcon buttonMenuIcon = Filling.createImageIcon("images/buttonMenu/buttonB.png");//инициализация иконок для кнопок
    ImageIcon buttonMenuIcon1 = Filling.createImageIcon("images/buttonMenu/buttonG.png");
    ImageIcon buttonMenuIcon2 = Filling.createImageIcon("images/buttonMenu/buttonR.png");
    boolean graphicFlag = false;
    public static volatile boolean thorFlag = true;

    public SuperButton(String title) { //конструктор задает начальные параметры кнопки
        super(title);
        setActionCommand(title);
        addActionListener(this);
        setIcon(buttonMenuIcon);
        setHorizontalTextPosition(JButton.CENTER);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setFont(new Font("Arial", Font.PLAIN, 24));
        setRolloverIcon(buttonMenuIcon2);
        setPressedIcon(buttonMenuIcon1);
        setSize(new Dimension(240, 80));
    }

    public void actionPerformed(ActionEvent e) {//обработчик кнопок
        Filling.audioClick = true;
        if ("Game".equals(e.getActionCommand())) {//если соответствует команде обработчика то переходим в режим игры
            Filling.audioGame = true;
            Filling.mode="Game";
        }
        if ("Settings".equals(e.getActionCommand())) {//если соответствует команде обработчика то переходим в режим настройки
            Filling.mode="Settings";
        }
        if ("About".equals(e.getActionCommand())) {//если соответствует команде обработчика  то переходим в режим ОБ ИГРЕ
            Filling.mode="About";
        }
        if ("Quit".equals(e.getActionCommand())) {//если соответствует команде обработчика  то переходим в режим выхода
            Filling.mode="Quit";
        }
        if ("Yes".equals(e.getActionCommand())) {//если соответствует команде обработчика
            Filling.audioClick = true;
            try {
                sleep(400);//задержка перед выходом, чтоб клик успел восроизвестись
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        }
        if ("Back to menu".equals(e.getActionCommand())||"No".equals(e.getActionCommand())) {//если соответствует команде обработчика
            Filling.audioMenu=true;
            Filling.mode="Menu";
        }
        if ("Enabled".equals(e.getActionCommand())) {//если соответствует команде обработчика
            Filling.audioEnabled = true;
        }
        if ("Thor".equals(e.getActionCommand())) {//если соответствует команде обработчика
            if(thorFlag){
                CreateField.mainFieldThor = false;
                thorFlag=false;
            }
            else {
                thorFlag =true;
                CreateField.mainFieldThor = true;
            }
        }
        if ("Disabled".equals(e.getActionCommand())) {//если соответствует команде обработчика
            Filling.audioEnabled = false;
        }
        if ("Change view".equals(e.getActionCommand())) {//если соответствует команде обработчика
            if(graphicFlag){
                Filling.border = false;
                graphicFlag=false;
            }
            else {
                graphicFlag =true;
                Filling.border = true;
            }
        }
    }
}
//Game
class DinamicScaledButton extends JButton { //создает кнопку с масштабированной иконкой под свой размер
    private Image scaled;
    private BufferedImage master;
    int width, height;

    public DinamicScaledButton(int width, int height, URL iconPath) {//конструктор кнопки-картинки
        this.width = width;
        this.height = height;
        setSize(width, height);
        setBorder(null);
        setContentAreaFilled(false);
        setIcon(iconResized(iconPath));
    }

    public DinamicScaledButton(int width, int height, String title) {//конструктор кнопки меню бара
        setFocusPainted(false);
        this.width = width;
        this.height = height;
        setSize(width, height);
        setBorder(null);
        setContentAreaFilled(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setText(title);
        setIcon(iconResized(this.getClass().getResource("images/buttonMenu/buttonB.png")));
        setPressedIcon(iconResized(this.getClass().getResource("images/buttonMenu/buttonG.png")));
        setRolloverIcon(iconResized(this.getClass().getResource("images/buttonMenu/buttonR.png")));
    }

    private ImageIcon iconResized(URL iconPath) { //возвращает масштабированную копию изображения
        Dimension size = this.getSize();
        Insets insets = this.getInsets();
        size.width -= insets.left + insets.right;
        size.height -= insets.top + insets.bottom;
        if (size.width > size.height) {
            size.width = -1;
        } else {
            size.height = -1;
        }
        try {
            master = ImageIO.read(iconPath);
            scaled = master.getScaledInstance(size.width, size.height, java.awt.Image.SCALE_SMOOTH);
        } catch (IOException ex) {ex.printStackTrace();}
        return new ImageIcon(scaled);
    }
}
//класс, заготовок фигур
class StandartShapes extends JPanel {
    JPanel fieldPanel=new JPanel();
    JPanel menuBar = new JPanel();
    final byte SIDE=32;
    boolean mas[][]=new boolean [SIDE][SIDE];//создание изначального поля (массив фиксирует состояние клеток)
    fieldButton buttons[][]=new fieldButton[SIDE][SIDE];//создание кнопок (они же клетки) для поля
    int DSBWidth = (int) (Filling.screenSize.width*0.085);
    int DSBHeight = (int) (Filling.screenSize.height*0.055);
    DinamicScaledButton leftBut = new DinamicScaledButton((int) (DSBWidth*1.5),(int) (DSBHeight*1.5), this.getClass().getResource("images/visualField/leftIcon.png"));
    DinamicScaledButton rightBut = new DinamicScaledButton((int) (DSBWidth*1.5),(int) (DSBHeight*1.5), this.getClass().getResource("images/visualField/rightIcon.png"));
    DinamicScaledButton newBut = new DinamicScaledButton(DSBWidth,DSBHeight,"New");
    DinamicScaledButton editBut = new DinamicScaledButton(DSBWidth,DSBHeight,"Edit");
    DinamicScaledButton saveBut = new DinamicScaledButton(DSBWidth,DSBHeight,"Save");
    DinamicScaledButton deleteBut = new DinamicScaledButton(DSBWidth,DSBHeight,"Delete");
    boolean masNotEmpty = false;
    ArrayList<boolean[][]> listOfMas = new ArrayList<>();
    int currentPosition = 0;
    JLabel pageLabel = new JLabel("Page №: 1");
    boolean currentAddMas[][]=new boolean [SIDE][SIDE];
    boolean disableField;
    int sideOfShapesField,positionOfShapesFieldX,positionOfShapesFieldY,yOfMenuBar,menuBarHeight;
    ControlPanel parent;
    DinamicScaledButton useBut = new DinamicScaledButton((int) (Filling.screenSize.width*0.085),(int) (Filling.screenSize.height*0.055),"Use");

    public StandartShapes() {
        setLayout(null);
        sideOfShapesField = (int) (Filling.screenSize.width*0.45);
        positionOfShapesFieldX = (int) sideOfShapesField/20;
        positionOfShapesFieldY = (int) sideOfShapesField/10;
        yOfMenuBar =(int) sideOfShapesField/199;
        menuBarHeight = Filling.screenSize.height/19;
        leftBut.setBounds(0, 0, 80,  60);
        rightBut.setBounds(sideOfShapesField-positionOfShapesFieldX, 0,100,  60);
        menuBar.setBounds(positionOfShapesFieldX, yOfMenuBar,sideOfShapesField,  menuBarHeight);
        fieldPanel.setBounds(positionOfShapesFieldX,positionOfShapesFieldY,sideOfShapesField,sideOfShapesField);
        fieldPanel.setBackground(null);
        fieldPanel.setLayout(new GridLayout(SIDE,SIDE));//добавляю в экземпляр панели "р" менеджер слоев с параметром нового табличного слоя
        listOfMas.add(mas);
        for(byte i=0;i<SIDE;i++)//заполнение поля панели путем создания экземпляров кнопки
            for(byte j=0;j<SIDE;j++) {
                buttons[i][j]=new fieldButton(i,j);//вместе с созданием передаю в новые экземпляры их координаты, для дальнейшего расположения
                fieldPanel.add(buttons[i][j]);//добавляю кнопки к панели
            }
        leftBut.setEnabled(false);
        showMas(listOfMas.get(0));
        disableButtons();//отключение доступа при первом запуске
        buttonActions();
        menuBar.add(newBut);
        menuBar.add(editBut);
        menuBar.add(saveBut);
        menuBar.add(deleteBut);
        menuBar.add(useBut);
        menuBar.setBackground(null);
        add(leftBut);
        add(fieldPanel);
        add(rightBut);
        add(menuBar);
        setBackground(Filling.color);
        readFromDB();
        showMas(listOfMas.get(0));
        currentAddMas=listOfMas.get(0);
        if(listOfMas.size()>1) {
            rightBut.setEnabled(true);
        }
    }

    public void useParent(ControlPanel parent) {
        this.parent = parent;
    }

    public void writeToDB () {
        if(listOfMas!=null) {
            DataBaseObjectWriter dbow = new DataBaseObjectWriter();
            dbow.set(listOfMas);
            dbow.write();
        }
    }

    public void readFromDB () {
        DataBaseObjectReader dbor = new DataBaseObjectReader();
        dbor.read();
        listOfMas = dbor.get();
    }

    public void buttonActions() {
        leftBut.setFocusable(false);
        leftBut.setBackground(null);
        leftBut.setEnabled(false);
        rightBut.setFocusable(false);
        rightBut.setBackground(null);
        rightBut.setEnabled(false);
        saveBut.setEnabled(false);
        useBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                parent.addToField();
            }
        });
        leftBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                newBut.setBackground(null);
                editBut.setBackground(null);
                saveBut.setBackground(null);
                deleteBut.setBackground(null);
                currentPosition--;
                if(currentPosition==0) {
                    leftBut.setEnabled(false);
                }
                if(listOfMas.size()>1)
                    rightBut.setEnabled(true);
                showMas(listOfMas.get(currentPosition));
                pageLabel.setText("Page №: "+(currentPosition+1));
                currentAddMas=listOfMas.get(currentPosition);
            }
        });
        rightBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                currentPosition++;
                if(currentPosition!=0) {
                    deleteBut.setEnabled(true);
                    editBut.setEnabled(true);
                }
                if(currentPosition+1==listOfMas.size())//выключаю кнопку если массив на последнем элементе
                    rightBut.setEnabled(false);
                if(currentPosition>0)
                    leftBut.setEnabled(true);
                showMas(listOfMas.get(currentPosition));
                pageLabel.setText("Page №: "+(currentPosition+1));
                currentAddMas=listOfMas.get(currentPosition);
            }
        });
        newBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                enableButtons();
                newBut.setBackground(Color.red);
                newBut.setEnabled(false);
                saveBut.setEnabled(true);
                rightBut.setEnabled(false);
                editBut.setEnabled(false);
                leftBut.setEnabled(false);
                deleteBut.setEnabled(false);
                useBut.setEnabled(false);
                listOfMas.add(new boolean[SIDE][SIDE]);
                currentPosition = listOfMas.size()-1;
                showMas(listOfMas.get(currentPosition));
            }
        });
        editBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                rightBut.setEnabled(false);
                leftBut.setEnabled(false);
                newBut.setEnabled(false);
                deleteBut.setEnabled(false);
                editBut.setEnabled(false);
                useBut.setEnabled(false);
                saveBut.setEnabled(true);
                enableButtons();
            }
        });
        saveBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(masNotEmpty()) {
                    leftBut.setEnabled(true);
                    editBut.setEnabled(true);
                    deleteBut.setEnabled(true);
                    newBut.setEnabled(true);
                    if(listOfMas.size()-1>currentPosition)
                        rightBut.setEnabled(true);
                    if(currentPosition==0) {
                        leftBut.setEnabled(false);
                    }
                    disableButtons();
                    saveBut.setBackground(Color.green);
                    newBut.setBackground(Color.green);
                    saveBut.setEnabled(false);
                    deleteBut.setEnabled(true);
                    useBut.setEnabled(true);
                    pageLabel.setText("Page №: "+(currentPosition+1));
                    currentAddMas=listOfMas.get(currentPosition);
                    writeToDB();
                }
            }
        });
        deleteBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(listOfMas.size()==1) {
                    listOfMas.clear();
                    listOfMas.add(new boolean [SIDE][SIDE]);
                    currentPosition=0;
                }
                if(listOfMas.size()>1) {
                    if(currentPosition==0) {
                        listOfMas.remove(currentPosition);
                        if(listOfMas.size()==1)
                            rightBut.setEnabled(false);
                    }
                    if(currentPosition+1==listOfMas.size()) {
                        listOfMas.remove(currentPosition);
                        currentPosition--;
                        rightBut.setEnabled(false);
                    }
                    else {
                        listOfMas.remove(currentPosition);
                        rightBut.setEnabled(true);
                        if(currentPosition+1==listOfMas.size())
                            rightBut.setEnabled(false);
                    }
                    showMas(listOfMas.get(currentPosition));
                    pageLabel.setText("Page №: "+(currentPosition+1));
                    currentAddMas=listOfMas.get(currentPosition);
                }
                writeToDB();
            }
        });
    }

    public boolean masNotEmpty () {
        for (byte i=0;i<SIDE;i++)
            for (byte j=0;j<SIDE;j++)
                if(listOfMas.get(currentPosition)[i][j]==true)
                    return true;
        return false;
    }

    public void showMas (boolean [][] arr) {//выводит поле на экран и считает к-во эл-в
        for (byte i=0;i<SIDE;i++) {
            for (byte j=0;j<SIDE;j++)
                if(arr[i][j]==true){
                    buttons[i][j].setBackground(Color.green);
                }
                else { //иначе - не отображать точку
                    buttons[i][j].setBackground(Color.red);
                }
        }
    }

    public void disableButtons() {//отключение кнопок
        disableField=true;
    }

    public void enableButtons() {
        disableField=false;
    }
    //класс, отвечающий за кнопку поля
    class fieldButton extends JButton implements ActionListener { //описывает кнопку поля
        byte i,j;

        public fieldButton(byte i, byte j){ //конструктор
            this.addActionListener(this);
            this.i=i;
            this.j=j;
        }

        public void actionPerformed(ActionEvent e) { //обработчик нажатия на поле
            if(!disableField) {
                setLife(i,j);//сетим массив
                if(listOfMas.get(currentPosition)[i][j]==true) {
                    setBackground(Color.green);
                }
                else {
                    setBackground(Color.red);
                }
            }
        }

        private void setLife(byte i, byte j) { //выставляет массив по вводу в поле
            if(listOfMas.get(currentPosition)[i][j]==false) listOfMas.get(currentPosition)[i][j]=true;
            else listOfMas.get(currentPosition)[i][j]=false;
        }
    }
}
//класс, выполняющий роль обьекта, при сериализации которого сохраняется динамический массив фигур
class DataBaseObject implements Serializable {
    private ArrayList<boolean[][]> mas;

    public void setMas(ArrayList<boolean[][]> mas) {
        this.mas = mas;
    }

    public ArrayList<boolean[][]> getMas() {
        return mas;
    }
}
//класс, производящий запись состояния обьекта в бинарный файл
class DataBaseObjectWriter {
    private DataBaseObject object;

    public void set(ArrayList<boolean[][]> listOfMas) {
        object = new DataBaseObject();
        object.setMas(listOfMas);
    }

    public void write() {
        if(object!=null) {
            try {
                FileOutputStream fos = new FileOutputStream("C:\\\\Program Files (x86)\\\\JavaLife\\\\DB.bin");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(object);
                oos.flush();
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            System.out.println("Object is empty");
    }
}
//класс, производящий чтение состояния обьекта из бинарного файла
class DataBaseObjectReader {//
    private DataBaseObject dbo;

    public ArrayList<boolean[][]> get() {
        if(dbo!=null) {
            return dbo.getMas();
        }
        return null;
    }

    public void read() {
        try {
            FileInputStream fis = new FileInputStream("C:\\\\Program Files (x86)\\\\JavaLife\\\\DB.bin");
            ObjectInputStream ois = new ObjectInputStream(fis);
            dbo = (DataBaseObject) ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
//класс, отвечающий за создание игрового поля
class CreateField extends JPanel//формирует поле
{
    JPanel p=new JPanel();//создание панели(контейнера)
    final byte SIDE=100;//размер стороны в клетках
    boolean mas[][]=new boolean [SIDE][SIDE];//создание изначального поля (массив фиксирует состояние клеток)
    fieldButton buttons[][]=new fieldButton[SIDE][SIDE];//создание кнопок (они же клетки) для поля
    public static boolean populationStabilized=false;
    int counterOfTrue = 0;//счетчик клеток
    ArrayList<Integer> populationDB = new ArrayList<Integer>();
    public static boolean populationDBReady = false;
    DrawGraphic drawGraphicPanel = new DrawGraphic();
    StandartShapes standartShapes = new StandartShapes();
    boolean currentAddMas[][];//массив, который станет массивом поля
    boolean fieldEnabled;//флаг включения поля
    public static volatile boolean mainFieldThor = true;//изначально тороидальное поле включено

    public CreateField()//конструктор
    {
        p.setLayout(new GridLayout(SIDE,SIDE));//добавляю в экземпляр панели "р" менеджер слоев с параметром нового табличного слоя
        for(byte i=0;i<SIDE;i++)//заполнение поля панели путем создания экземпляров кнопки
            for(byte j=0;j<SIDE;j++) {
                buttons[i][j]=new fieldButton(i,j);//вместе с созданием передаю в новые экземпляры их координаты, для дальнейшего расположения
                buttons[i][j].setBackground(Color.blue);
                p.add(buttons[i][j]);//добавляю кнопки к панели
            }
        showMas();//вывожу поле на экран
        disableButtons();//отключение доступа при первом запуске
    }

    public void disableButtons() {//отключение кнопок
        fieldEnabled = false;
    }

    public void enableButtons() {//включение кнопок
        fieldEnabled = true;
    }

    public void goLife()  {//выполняет начальное заполнение - создание пустого поля(массива)
        for (byte i=0;i<SIDE;i++)
            for (byte j=0;j<SIDE;j++)
                mas[i][j]=false;
    }

    public void border () {
        if(populationDBReady) {
            drawGraphicPanel.setMas(populationDB);
        }
    }

    public boolean masNotEmpty (){
        for (byte i=0;i<SIDE;i++)
            for (byte j=0;j<SIDE;j++)
                if(currentAddMas[i][j]==true)
                    return true;
        return false;
    }

    public void showMas () //выводит поле на экран и считает к-во эл-в
    {
        populationDBReady = false;
        for (byte i=0;i<SIDE;i++) {
            for (byte j=0;j<SIDE;j++)
                if(mas[i][j]==true) {
                    buttons[i][j].setBackground(Color.green);
                    counterOfTrue++;
                }
                else  {//иначе - не отображать картинку
                    buttons[i][j].setBackground(Color.blue);
                }
        }
        populationDB.add(counterOfTrue);//добавляю в БД к-во элементов
        counterOfTrue = 0;
        populationDBReady = true;//БД готова к считыванию
    }

    public void startLife() {//моделирование условий жизни клетки
        boolean currentAddMas[][]=new boolean [SIDE][SIDE];//массив, который станет полем на следующем шаге

        for (byte i=0; i<SIDE; i++) {//подсчет соседей на изначальном поле с учетом краевых условий и формирование следующего поля в цикле
            for (byte j=0; j<SIDE; j++) {
                byte neighbors = 0;//счетчик соседей
                if(mainFieldThor) {//тор
                    if (mas[fpm(i)-1][j]==true) neighbors++;//верх
                    if (mas[i][fpp(j)+1]==true) neighbors++;//право(для последнего элемента в ряду правым соседом будет первый элемент в этом же ряду)
                    if (mas[fpp(i)+1][j]==true) neighbors++;//низ
                    if (mas[i][fpm(j)-1]==true) neighbors++;//лево
                    if (mas[fpm(i)-1][fpp(j)+1]==true) neighbors++;//верх-право
                    if (mas[fpp(i)+1][fpp(j)+1]==true) neighbors++;//низ-право
                    if (mas[fpp(i)+1][fpm(j)-1]==true) neighbors++;//низ-лево
                    if (mas[fpm(i)-1][fpm(j)-1]==true) neighbors++;//верх-лево
                } else {//не тор
                    if(i==0) {//если верхний
                        if(!(j==SIDE-1)) {//если не крайний правый
                            if (mas[i][j+1]==true) neighbors++;//право
                            if (mas[i+1][j+1]==true) neighbors++;//низ-право
                        }
                        if(!(j==0)) {//если не крайний левый
                            if (mas[i][j-1]==true) neighbors++;//лево
                            if (mas[i+1][j-1]==true) neighbors++;//низ-лево
                        }
                        if (mas[i+1][j]==true) neighbors++;//низ

                    } else
                    if(i==SIDE-1) {//если нижний
                        if(!(j==SIDE-1)) {//если не крайний правый
                            if (mas[i][j+1]==true) neighbors++;//право
                            if (mas[i-1][j+1]==true) neighbors++;//верх-право
                        }
                        if(!(j==0)) {//если не крайний левый
                            if (mas[i][j-1]==true) neighbors++;//лево
                            if (mas[i-1][j-1]==true) neighbors++;//верх-лево
                        }
                        if (mas[i-1][j]==true) neighbors++;//верх
                    } else {
                        if(!(j==SIDE-1)&&!(j==0)) {//если не крайний левый и не крайний правый
                            if (mas[i][j+1]==true) neighbors++;//право
                            if (mas[i-1][j+1]==true) neighbors++;//верх-право
                            if (mas[i-1][j]==true) neighbors++;//верх
                            if (mas[i-1][j-1]==true) neighbors++;//верх-лево
                            if (mas[i][j-1]==true) neighbors++;//лево
                            if (mas[i+1][j-1]==true) neighbors++;//низ-лево
                            if (mas[i+1][j]==true) neighbors++;//низ
                            if (mas[i+1][j+1]==true) neighbors++;//низ-право
                        }
                        if(j==SIDE-1) {//если крайний правый, не угловой
                            if (mas[i-1][j]==true) neighbors++;//верх
                            if (mas[i-1][j-1]==true) neighbors++;//верх-лево
                            if (mas[i][j-1]==true) neighbors++;//лево
                            if (mas[i+1][j-1]==true) neighbors++;//низ-лево
                            if (mas[i+1][j]==true) neighbors++;//низ
                        }
                        if(j==0) {//если крайний левый, не угловой
                            if (mas[i+1][j]==true) neighbors++;//низ
                            if (mas[i+1][j+1]==true) neighbors++;//низ-право
                            if (mas[i][j+1]==true) neighbors++;//право
                            if (mas[i-1][j+1]==true) neighbors++;//верх-право
                            if (mas[i-1][j]==true) neighbors++;//верх
                        }
                    }
                }
                if (mas[i][j]==false && neighbors==3) currentAddMas[i][j]=true;//если у мертвой клетки ровно 3 соседа она оживает
                else currentAddMas[i][j]=false;//иначе - клетка умирает
                if (mas[i][j]==true && (neighbors==2 || neighbors==3)) currentAddMas[i][j]=true;//если у клетки 2 или 3 соседа и она была жива - она остается жива
            }
        }
        mas = currentAddMas;//просчитаное поле становится текущим полем
        showMas();//вывожу поле на экран
    }

    private byte fpm(byte i)  {//краевое условие для создания поверхности Тора вверху и слева(minus)
        if(i==0) return SIDE;//если дошел до минимального края - вернуть край другой стороны
        else return i;
    }

    private byte fpp(byte i) {//краевое условие для создания поверхности Тора внизу и справа(plus)
        if(i==SIDE-1) return -1;
        else return i;
    }
    //класс кнопки игрового поля
    class fieldButton extends JButton implements ActionListener {//описывает кнопку поля
        byte i,j;

        public fieldButton(byte i, byte j) {//конструктор
            this.addActionListener(this);
            this.i=i;
            this.j=j;
        }

        public void actionPerformed(ActionEvent e) {//обработчик нажатия на поле
            if(fieldEnabled) {
                setLife(i,j);
                if(mas[i][j]== true) {
                    buttons[i][j].setBackground(Color.green);
                }
                else {
                    buttons[i][j].setBackground(Color.blue);
                }
            }
        }

        private void setLife(byte i, byte j) {//выставляет массив по вводу в поле
            if(mas[i][j]==false) mas[i][j]=true;
            else mas[i][j]=false;
        }
    }
}
//класс управляющих элементов
class ControlPanel extends JFrame implements ActionListener  {//здесь описание интерфейса управления!
    JButton buttonPlayPause, buttonStop, buttonSelect, buttonFaster, buttonSlower;//обьявление кнопок
    JLabel labelPeriod = new JLabel("Period, ms"); //обьявление надписей
    JLabel labelGeneration  = new JLabel("Generation: ");
    boolean clickedPlayPause = true;//флаг для кн PlayPause
    ImageIcon buttonPlayIcon = Filling.createImageIcon("images/visualPlayer/play.png");//инициализация иконок для кнопок
    ImageIcon buttonPauseIcon = Filling.createImageIcon("images/visualPlayer/pause.png");
    ImageIcon buttonStopIcon = Filling.createImageIcon("images/visualPlayer/stop.png");
    ImageIcon buttonSelectIcon = Filling.createImageIcon("images/visualPlayer/select.png");
    ImageIcon buttonFasterIcon = Filling.createImageIcon("images/visualPlayer/faster.png");
    ImageIcon buttonSlowerIcon = Filling.createImageIcon("images/visualPlayer/slower.png");
    JPanel p=new JPanel();//создание панели(контейнера)
    CreateField createField = new CreateField();//создание экземпляра CreateField
    byte DELAY = 10;//период таймера
    byte Ccount=5;//счетчик для  периода цикла, 10*100 мс =1c/цикл
    byte count=Ccount;//счетчик-итератор
    int generationСounter=0;//счетчик поколений
    SuperButton backToMenuBut = new SuperButton("Back to menu");
    SuperButton backToMenuBut2 = new SuperButton("Back to menu");
    SuperButton changeViewBut = new SuperButton("Change view");
    int DSBWidth = (int) (Filling.screenSize.width*0.085);
    int DSBHeight = (int) (Filling.screenSize.height*0.055);
    ArrayList<JButton> components = new ArrayList<JButton>();
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel populationGrapficPanel = new JPanel();
    JPanel buttonBar = new JPanel();
    public static Dimension dimOfGraphic;
    JLabel labelMaxPopulation = new JLabel("MAX");
    JLabel labelCurrentPopulation = new JLabel("Current population:  ");
    float timeExecution;

    public ControlPanel()//конструктор
    {
        buttonPlayPause = new JButton("Play/Pause", buttonPlayIcon);//создаю кнопку
        buttonPlayPause.setMnemonic(KeyEvent.VK_Q);//подключаю нажатие на кнопку сочитанием клавиш ALT+Q
        buttonPlayPause.setActionCommand("actionPlayPause");//команда по нажатию
        buttonPlayPause.addActionListener(this);//слушатели для действий кнопок
        buttonPlayPause.setToolTipText("ALT + Q");//вывод подсказок
        buttonSelect = new JButton("Select", buttonSelectIcon);
        buttonSelect.setMnemonic(KeyEvent.VK_W);
        buttonSelect.setActionCommand("actionSelect");
        buttonSelect.addActionListener(this);
        buttonSelect.setToolTipText("ALT + W");
        buttonStop = new JButton("Stop", buttonStopIcon);
        buttonStop.setMnemonic(KeyEvent.VK_E);
        buttonStop.setActionCommand("actionStop");
        buttonStop.addActionListener(this);
        buttonStop.setToolTipText("ALT + E");
        buttonFaster = new JButton("Faster", buttonFasterIcon);
        buttonFaster.setMnemonic(KeyEvent.VK_A);
        buttonFaster.setActionCommand("actionFaster");
        buttonFaster.addActionListener(this);
        buttonFaster.setToolTipText("ALT + A");
        buttonSlower = new JButton("Slower", buttonSlowerIcon);
        buttonSlower.setMnemonic(KeyEvent.VK_D);
        buttonSlower.setActionCommand("actionSlower");
        buttonSlower.addActionListener(this);
        buttonSlower.setToolTipText("ALT + D");
        components.add(buttonFaster);
        components.add(buttonPlayPause);
        components.add(buttonSelect);
        components.add(buttonStop);
        components.add(buttonSlower);
        for(JButton component: components) {
            component.setBackground(null);
            component.setBorder(null);
            p.add(component);
        }
        labelPeriod.setText("Period, ms: " + Ccount*DELAY);//вывод периода таймера
        labelGeneration.setText("Generation: " + generationСounter);//вывод поколения
        labelGeneration.setHorizontalAlignment((int)CENTER_ALIGNMENT);
        p.setLayout(null);
        p.add(labelPeriod);
        p.add(labelGeneration);
        p.add(backToMenuBut);
        p.add(changeViewBut);
        p.add(backToMenuBut2);
        Insets insets = p.getInsets();
        Dimension size = backToMenuBut.getSize();
        backToMenuBut.setBounds(400 + insets.left, 220 + insets.top, size.width, size.height);
        backToMenuBut2.setBounds(400 + insets.left, 220 + insets.top, size.width, size.height);
        changeViewBut.setBounds(400 + insets.left, 340 + insets.top,size.width, size.height);
        size = buttonPlayPause.getPreferredSize();
        buttonPlayPause.setBounds(50 + insets.left, 220 + insets.top,size.width, size.height);
        size = buttonStop.getPreferredSize();
        buttonStop.setBounds(50 + insets.left,320 + insets.top,size.width, size.height);
        size = buttonSelect.getPreferredSize();
        buttonSelect.setBounds(50 + insets.left, 420 + insets.top,size.width, size.height);
        size = buttonFaster.getPreferredSize();
        buttonFaster.setBounds(440 + insets.left, 220 + insets.top,size.width, size.height);
        size = buttonSlower.getPreferredSize();
        buttonSlower.setBounds(440 + insets.left, 420 + insets.top,size.width, size.height);
        labelPeriod.setBounds(440 + insets.left, 320 + insets.top,size.width, size.height);
        createField.standartShapes.useParent(this);
        populationGrapficPanel.setLayout(null);
        createField.drawGraphicPanel.setLocation(30, 30);
        createField.drawGraphicPanel.setSize((int)(Toolkit.getDefaultToolkit().getScreenSize().width/2-62), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/4*3)-70);
        populationGrapficPanel.add(createField.drawGraphicPanel);
        populationGrapficPanel.add(labelGeneration);
        labelGeneration.setSize(100, 30);
        labelGeneration.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/2-62)-50, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/4*3)-40);
        labelMaxPopulation.setLocation(0, 20);
        labelMaxPopulation.setSize(30, 30);
        populationGrapficPanel.add(labelMaxPopulation);
        populationGrapficPanel.add(labelCurrentPopulation);
        labelCurrentPopulation.setLocation(250, 0);
        labelCurrentPopulation.setSize(150, 30);
        buttonBar.setLayout(null);
        buttonBar.add(changeViewBut);
        changeViewBut.setLocation(10, 10);
        buttonBar.add(backToMenuBut);
        backToMenuBut.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/2)-260, 10);
        backToMenuBut2.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/2)-260, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/4*3)+10);
        buttonBar.setLocation(0, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/4*3));
        buttonBar.setSize((int)(Toolkit.getDefaultToolkit().getScreenSize().width/2), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/2.2));
        populationGrapficPanel.add(buttonBar);
        buttonBar.setBackground(Filling.color);
        populationGrapficPanel.setBackground(Filling.color);
        tabbedPane.addTab("Controls", null,p,"Field Control");
        tabbedPane.addTab("Standard Shapes", null,createField.standartShapes,"Try Preset Shapes");
        tabbedPane.addTab("Population Grapfic", null,populationGrapficPanel,"Show How Growth Population");
        tabbedPane.setBackground(Filling.color);
        tabbedPane.setBorder(null);
        tabbedPane.setBackgroundAt(0, Filling.color);
        tabbedPane.setBackgroundAt(1, Filling.color);
        tabbedPane.setBackgroundAt(2, Filling.color);
    }

    Timer timer = new Timer(DELAY, new ActionListener() {//создание экзампляра Timer с указанием задержки и подключением Слушателя событий
        public synchronized void actionPerformed(ActionEvent evt) {//выполнение действия по событию
            TimeCounter();
        }
    });

    private void TimeCounter() {//счетчик
        count--;
        if(count==0) {
            createField.startLife();//моделирование условий жизни клетки
            createField.border();
            generationСounter++;//увеличиваем счетчик поколений
            labelGeneration.setText("Generation: " + generationСounter); //вывод поколения
            labelMaxPopulation.setText(""+createField.drawGraphicPanel.max);
            labelCurrentPopulation.setText("Current population: "+createField.drawGraphicPanel.current);
            timeExecution+=Ccount*10;
            count=Ccount;//установка итератора в начальное значение
        }
    }

    private void TimeInc() {//увеличивает период
        if(Ccount<100)//< 1 s
            Ccount++;
    }

    private void TimeDec() {//уменьшает период
        if(Ccount>1)//>10 ms
            Ccount--;
    }

    public synchronized void actionPerformed(ActionEvent e)  {//обработчик кнопок
        if ("actionPlayPause".equals(e.getActionCommand()))  {//если соответствует команде обработчика
            if(clickedPlayPause)  {//флаг, чтоб реализовать PlayPause на 1 кнопке
                buttonPlayPause.setIcon(buttonPauseIcon);
                buttonSelect.setEnabled(false);
                clickedPlayPause = false;
                timer.start();
                createField.disableButtons();
            } else {
                buttonPlayPause.setIcon(buttonPlayIcon);
                buttonSelect.setEnabled(true);
                clickedPlayPause = true;
                timer.stop();
                createField.disableButtons();
            }
        } else if ("actionStop".equals(e.getActionCommand()))  {//если соответствует команде обработчика
            timer.stop();//остановка таймера
            buttonPlayPause.setIcon(buttonPlayIcon);//установка иконки Play
            buttonSelect.setEnabled(true);//разрешаю редактирование поля
            clickedPlayPause = true;//устанавливаю флаг нажатия для PlayPause
            createField.goLife();//очистка массива поля
            createField.showMas();//показываю поле
            generationСounter=0;
            labelGeneration.setText("Generation: ");
            createField.drawGraphicPanel.deleteMas();
            createField.drawGraphicPanel.max=0;
            labelMaxPopulation.setText("MAX");
            createField.drawGraphicPanel.current=0;
            labelCurrentPopulation.setText("Current population:  ");

        } else if ("actionSelect".equals(e.getActionCommand())) {//если соответствует команде обработчика
            createField.enableButtons();
        } else if ("actionFaster".equals(e.getActionCommand())) {//если соответствует команде обработчика
            TimeDec();
            labelPeriod.setText("Period, ms: " + Ccount*DELAY);
        } else if ("actionSlower".equals(e.getActionCommand())) {//если соответствует команде обработчика
            TimeInc();
            labelPeriod.setText("Period, ms: " + Ccount*DELAY);
        }
    }

    public void addToField() {
        currentAddMasLocate();
        createField.showMas();
    }
    public void currentAddMasLocate() {
        byte sizeShape = createField.standartShapes.SIDE;
        byte indent = (byte) (createField.SIDE/2 - sizeShape/2);//(10000^1/2)/2-(1024^1/2)/2=50-16=34 - начало координат вставки фигуры (по-центру)
        for (byte i=0;i<sizeShape;i++)
            for (byte j=0;j<sizeShape;j++)
                if(createField.standartShapes.currentAddMas[i][j]==true)
                    createField.mas[i+indent][j+indent]=true;
    }
}
//класс, рисующий график
class DrawGraphic extends JComponent {
    ArrayList<Integer> chartList;
    int width,height,max,current;

    public void setMas(ArrayList<Integer> l){
        chartList = l;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        width = getWidth();
        height = getHeight();
        Graphics2D graphics2d = (Graphics2D)g;
        graphics2d.setColor(Color.white);//начальная заливка поля графика
        graphics2d.fillRect(0, 0, width, height);
        if(chartList != null&&chartList.size()>0)
            paintMe(graphics2d);
    }

    private void paintMe(Graphics2D graphics2d){
        float hDiv;
        graphics2d.setColor(Color.blue);
        float vDiv = (float)height/(float)(Collections.max(chartList));
        max = Collections.max(chartList);
        for(int i=0,j=0; i<chartList.size()-1; i++) {
            current = chartList.get(i);
            if(Filling.border) {
                hDiv = 1f;
                if(i%width==0){
                    graphics2d.setColor(Color.white);//заливка поля графика для следующего фрейма
                    graphics2d.fillRect(0, 0, width, height);
                    graphics2d.setColor(Color.blue);
                    j=0;
                }
            } else hDiv = (float)width/(float)(chartList.size()-1);
            int value1, value2;
            if(chartList.get(i)==null)
                value1 = 0;
            else
                value1 = chartList.get(i);
            if(chartList.get(i+1)==null)
                value2 = 0;
            else
                value2 = chartList.get(i+1);
            graphics2d.drawLine((int)(j*hDiv),height - ((int)(value1*vDiv)),(int)((j+1)*hDiv),height - ((int)(value2*vDiv)));
            j++;
        }
    }

    void deleteMas() {
        chartList.removeAll(chartList);
        repaint();
    }
}
//Настройки
class Settings {
    JPanel panel = new JPanel();
    JLabel thorStateLabel = new JLabel();
    JLabel soundStateLabel = new JLabel();
    SuperButton b1 = new SuperButton("Back to menu");
    SuperButton b4 = new SuperButton("Enabled");
    SuperButton b5 = new SuperButton("Disabled");
    SuperButton b6 = new SuperButton("Thor");

    public Settings() {
        panel.setLayout(null);
        b1.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/4*3)-100, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*4)-100);
        int sizeX = 300;
        int sizeY = 40;
        thorStateLabel.setText("Thor mode is ON");
        thorStateLabel.setFont(new Font("Arial", Font.PLAIN, 30));
        thorStateLabel.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().width/6*1)+10, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*2+120), sizeX, sizeY);
        b6.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/6*1), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*4)-100);
        b4.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/6*1), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*2)-100);
        b5.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/5*2)-20, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*2)-100);
        soundStateLabel.setFont(new Font("Arial", Font.PLAIN, 30));
        soundStateLabel.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().width/6*2), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/5*1), sizeX, sizeY);
        soundStateLabel.setText("Sound is ON");
        panel.add(b1);
        panel.add(b4);
        panel.add(b5);
        panel.add(b6);
        panel.add(thorStateLabel);
        panel.add(soundStateLabel);
    }
}
//Об игре
class About {
    JPanel panel = new JPanel();
    JLabel content = new JLabel("JavaLife v 1.0");
    JLabel content2 = new JLabel("Developer: Ruslan Kutugin");
    JLabel content3 = new JLabel("E-mail: Reneuby.Heckfy@gmail.com");
    SuperButton b1 = new SuperButton("Back to menu");

    public About() {
        panel.setLayout(null);
        panel.add(content);
        int sizeX = content.getPreferredSize().width*3;
        int sizeY = 40;
        content.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width/2-sizeX/2, Toolkit.getDefaultToolkit().getScreenSize().height/5*1, sizeX, sizeY);
        sizeX = content2.getPreferredSize().width*3;
        content2.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width/2-sizeX/2, Toolkit.getDefaultToolkit().getScreenSize().height/5*2, sizeX, sizeY);
        sizeX = content3.getPreferredSize().width*3;
        content3.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width/2-sizeX/2, Toolkit.getDefaultToolkit().getScreenSize().height/5*3, sizeX, sizeY);
        b1.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/4*3)-100, (int)(Toolkit.getDefaultToolkit().getScreenSize().height/2)-100);
        content.setFont(new Font("Arial", Font.PLAIN, 30));
        content2.setFont(new Font("Arial", Font.PLAIN, 30));
        content3.setFont(new Font("Arial", Font.PLAIN, 30));
        panel.add(content2);
        panel.add(content3);
        panel.add(b1);
    }
}
//Выход
class Quit {
    JPanel panel = new JPanel();
    JLabel sure = new JLabel("Are you sure?");
    SuperButton b1 = new SuperButton("Yes");
    SuperButton b2 = new SuperButton("No");

    public Quit() {
        panel.setLayout(null);
        b1.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/7*1), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/2)-100);
        b2.setLocation((int)(Toolkit.getDefaultToolkit().getScreenSize().width/7*5), (int)(Toolkit.getDefaultToolkit().getScreenSize().height/2)-100);
        int sizeX = sure.getPreferredSize().width*3;
        int sizeY = 40;
        sure.setFont(new Font("Arial", Font.PLAIN, 30));
        sure.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width/2-sizeX/2, Toolkit.getDefaultToolkit().getScreenSize().height/3*1, sizeX, sizeY);
        panel.add(b1);
        panel.add(b2);
        panel.add(sure);
    }
}
//класс, отвечающий за работу со звуками (При отсутствии звука файлы (clickButton.wav, menu.wav, game.wav) пооложить в директорию "C:\\JavaLife\\имя файла")
class Sounds implements Runnable {
    Clip clipGame,clipClickButton,clipMenu;

    public void run() {
        while (true) {
            if(Filling.audioEnabled) {
                if("Menu".equals(Filling.mode)) {
                    if(Filling.audioMenu) {
                        Filling.audioMenu=false;
                        playMenu();
                    }
                }
                else stopMenu();
                try {
                    sleep(10);
                } catch (InterruptedException e) {e.printStackTrace();}
                if("Game".equals(Filling.mode)) {
                    if(Filling.audioGame) {
                        playGame();
                        Filling.audioGame=false;
                    }
                }
                else stopGame();
                if(Filling.audioClick) {
                    playClickButton();
                    Filling.audioClick=false;
                }
            }
        }
    }

    public void playClickButton() {
        new Thread() {
            public  void run() {
                try {
                    File soundFile = new File("C:\\Program Files (x86)\\JavaLife\\clickButton.wav"); //считываем файл
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);    //загружаем поток
                    clipClickButton = AudioSystem.getClip();//загрузка проигрывателя
                    clipClickButton.open(audioIn);//открытие файла
                    clipClickButton.start();//запуск воспроизвидения
                } catch (UnsupportedAudioFileException e) {//вывод сообщений об исключительной ситуации
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void playMenu() {
        new Thread() {
            public  void run() {
                try {
                    File soundFile = new File("C:\\Program Files (x86)\\JavaLife\\menu.wav");
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    clipMenu = AudioSystem.getClip();
                    clipMenu.open(audioIn);
                    clipMenu.start();
                    clipMenu.loop(Integer.MAX_VALUE);//повтор музыки
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void stopMenu() {//остановка воспроизведения
        if(clipMenu!=null) {
            clipMenu.stop();
            clipMenu.close();
            Filling.audioMenu=false;
        }
    }

    public void playGame() {
        new Thread() {
            public  void run() {
                try {
                    File soundFile = new File("C:\\Program Files (x86)\\JavaLife\\game.wav");
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    clipGame = AudioSystem.getClip();
                    clipGame.open(audioIn);
                    clipGame.start();
                    clipGame.loop(Integer.MAX_VALUE);//повтор музыки
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void stopGame() {//остановка воспроизведения
        if(clipGame!=null) {
            clipGame.stop();
            clipGame.close();
            Filling.audioGame=false;
        }
    }
}