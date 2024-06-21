package src.com.textreader.comic;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import src.com.textreader.comic.*;
public class ComicObject {
    private boolean fast_mode = false;
    private String file = "";
    private List<ComicPage> index = new ArrayList<>();
    private ComicINFO INFO = new ComicINFO();
    public byte[] get_image_data(int data_id){//id从1开始
        System.out.println(data_id);
        if(data_id<0) return "ERR".getBytes();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            int i = -1;
            while ((line = reader.readLine()) != null) {
                if(i>=0){ i++; }else if(line.contains("-LIST-ENDL")) { i++; }
                if(i >= data_id+1) return Base64.getDecoder().decode(line);
            }
            System.out.println(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "ERR".getBytes();
    }
    public List<ComicPage> GetIndex(){
        if(fast_mode) return new ArrayList<>();
        return this.index;
    }
    public ComicINFO GetInfo(){
        return this.INFO;
    }

    /**
     * 注意:如果要频繁调用且不需要获取信息,请使用fast_open()函数
     * @param FileName 要打开的文件名
     * @return 是否成功
     */
    public boolean open(String FileName){
        file = FileName;
        boolean start = false;
        int size = 0;
        int id = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            while ((line = reader.readLine()) != null) {
                id++;
                if(id == 1) INFO.setTittle(line);
                if(id == 2) INFO.setAuthor(line);
                if(id == 3) INFO.setProfile(line);
                if(id == 4) INFO.setIcon(Base64.getDecoder().decode(line));
                if (line.equals("-INFO-ENDL")){
                    start = true;
                }
                if (line.equals("-LIST-ENDL")) break;
                if(start){
                    if (line.charAt(0) == '[') {
                        String[] d = line.substring(1).split("\\|");
                        if(d.length>=2){
                            ComicPage v = new ComicPage();
                            v.setTitle(d[0]);
                            int s = Integer.parseInt(d[1]);
                            for (int k = 0; k < s; k++) {
                                v.push(size+k);
                            }
                            size+=s;
                            this.index.add(v);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return !start;
    }
    /**
     * 注意:如果需要获取信息,请使用open()函数
     * @param FileName 要打开的文件名
     */
    public void fast_open(String FileName){
        fast_mode = true;
        file = FileName;
        INFO.setTittle("**FAST OPEN**");
        INFO.setProfile("Warning: Fast open mode can not read this info!!");
        INFO.setAuthor("**FAST OPEN**");
        INFO.setIcon("**FAST OPEN**".getBytes());
    }
}
