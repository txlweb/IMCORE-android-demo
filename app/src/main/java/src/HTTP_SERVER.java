package src;

import static com.idsoft.imcore_android_demo.ct.*;

import src.com.textreader.comic.ComicINFO;
import src.com.textreader.comic.ComicMake;
import src.com.textreader.comic.ComicObject;
import src.com.textreader.comic.ComicPage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static src.com.textreader.comic.ComicMake.deleteFileByIO;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

import com.idsoft.imcore_android_demo.MainActivity;

public class HTTP_SERVER {
    public static boolean run_lock = true;
    public static boolean debug = false;

    public static void run() {
        int port = 8080;
        run_lock = true;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println((char) 27 + "[34m[I]: 网页服务正运行在" + port + "上 (http://127.0.0.1:" + port + ")" + (char) 27 + "[39;49m");
            while (run_lock) {
                Socket client = server.accept();
                RequestHandler handler = new RequestHandler(client);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println((char) 27 + "[41;39m[I]: 发生意外的数据发送终止(于图片服务上),这不影响使用." + (char) 27 + "[39;49m");
        run();//自动重启
    }



}
class RequestHandler implements Runnable {
    private final Socket client;
    public RequestHandler(Socket client) {
        this.client = client;
    }
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            String requestLine = in.readLine();
            String[] parts = requestLine.split(" ");
            String path = parts[1];
            if(path.contains("/get_im?")){
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                a = a[1].split("&");
                //FILE&INDEX
                ComicObject o = new ComicObject();
                //下面的注释是已经在快速读取中弃用的方法.
                //if(o.open(a[0])){ sendText(out,"!! ERROR !!");return;}
                o.fast_open(a[0]);
                sendResponse(out, 200, "OK", "image/jpeg", o.get_image_data(Integer.parseInt(a[1])));
                return;
            }
            if(path.contains("/get_ic?")){
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                a = a[1].split("&");
                //FILE&INDEX
                ComicObject o = new ComicObject();
                if(o.open(a[0])){ sendText(out,"!! ERROR !!");return;}
                sendResponse(out, 200, "OK", "image/jpeg", o.GetInfo().getIcon());
                return;
            }
            if(path.contains("/get_index?")){
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                a = a[1].split("&");
                //FILE
                ComicObject o = new ComicObject();
                if(o.open(a[0])){ sendText(out,"!! ERROR !!");return;}
                //PRE_JSON
                List<ComicPage> p = o.GetIndex();
                String r = "{";
                ComicINFO inf = o.GetInfo();
                r=r+"\"title\":\""+inf.getTittle()+"\",";
                r=r+"\"info\":\""+inf.getProfile()+"\",";
                r=r+"\"author\":\""+inf.getAuthor()+"\",";
                r = r+"\"data\":[";
                //System.out.println(p.size());
                for (ComicPage comicPage : p) {
                    r = r + "{\"title\":\""+comicPage.getTitle()+ "\",\"data\":[";
                    for (int j = 0; j < comicPage.size(); j++) {
                        r=r+"\"/get_im?"+a[0]+"&"+(j+1)+"\",";
                    }
                    r=r.substring(0,r.length()-1);
                    r=r+"]},";
                }
                r=r.substring(0,r.length()-1);
                r=r+"]}";
                r=r.replace("\\","\\\\");
                sendText(out,r);
                return;
            }
            if(path.contains("/build.html")) {
                sendText(out,"<meta charset=\"UTF-8\">\n" +
                        "<div class=\"center-container\">\n" +
                        "    <img src=\"/bic.jpg\" alt=\"SYNCING...\" class=\"center-image\">\n" +
                        "    <p class=\"center-text\">正在扫描压缩文件...</p>\n" +
                        "</div>\n" +
                        "<style>\n" +
                        "    .center-container {\n" +
                        "        display: flex;\n" +
                        "        flex-direction: column; /* 设置为列布局 */\n" +
                        "        justify-content: center; /* 垂直居中 */\n" +
                        "        align-items: center; /* 水平居中（在列布局中，这实际上不会影响图片或文字的水平位置） */\n" +
                        "        height: 100%; /* 使得容器占据整个视口的高度 */\n" +
                        "        text-align: center; /* 使文字在图片下方居中 */\n" +
                        "        margin: 0; /* 去除任何可能的边距 */\n" +
                        "    }\n" +
                        "\n" +
                        "    .center-image {\n" +
                        "        width: 40%; /* 防止图片过大 */\n" +
                        "        height: auto; /* 保持图片的原始比例 */\n" +
                        "    }\n" +
                        "\n" +
                        "    .center-text {\n" +
                        "        margin-top: 10px; /* 根据需要调整图片和文字之间的间距 */\n" +
                        "    }\n" +
                        "</style>\n" +
                        "<script>\n" +
                        "    var xhr = new XMLHttpRequest();\n" +
                        "    xhr.open(\"GET\", \"/build\", true);\n" +
                        "    xhr.onreadystatechange = function () {\n" +
                        "        if (xhr.readyState === 4 && xhr.status === 200) {\n" +
                        "            window.location.href = \"/\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "    xhr.send();\n" +
                        "</script>");
                return;
            }
            if(path.contains("/build")) {
                try {
                    List<String> filePaths = Files.walk(Paths.get(settings.sync_path))
                            .filter(Files::isRegularFile) // 过滤出文件，排除目录
                            .map(Path::toAbsolutePath) // 转换为绝对路径
                            .map(Path::toString) // 转换为字符串
                            .collect(Collectors.toList()); // 收集到列表中
                    for (int i = 0; i < filePaths.size(); i++) {
                        if (filePaths.get(i).contains(".zip") || filePaths.get(i).contains(".ZIP") && !(filePaths.get(i).contains(".CEIP") || filePaths.get(i).contains(".ceip"))){
                            if(!new File(filePaths.get(i)+".CEIP").isFile()) {
                                ComicMake.auto_make(filePaths.get(i) + ".CEIP", filePaths.get(i), settings.tmp_path);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendText(out,"build");
                return;
            }
            if(path.contains("/io/unpk")) {
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                a = a[1].split("&");
                String r ="{\"files\":[";
                try {
                    deleteFileByIO(new File(settings.tmp_path+"/tmp").getAbsolutePath());
                    List<String> filePaths = Files.walk(Paths.get(settings.tmp_path+"/tmp"))
                            .filter(Files::isRegularFile) // 过滤出文件，排除目录
                            .map(Path::toAbsolutePath) // 转换为绝对路径
                            .map(Path::toString) // 转换为字符串
                            .collect(Collectors.toList()); // 收集到列表中
                    for (int i = 0; i < filePaths.size(); i++) {
                        r=r+"\""+filePaths.get(i)+"\",";
                    }
                    r=r.substring(0,r.length()-1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                r=r+"]}";
                sendText(out,r);
                return;
            }
            if(path.contains("/io/pkg")) {
                //title&anu&tip&ic&
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                a = a[1].split("&");
//                形参:
//                save_to – 保存位置 img_dir – 图片列表(顺序) ic_img_dir – 图标 title – 标题 author – 作者 profile – 简介 chapter – 章节列表(new Array()为生成一整个)
//                返回值:
//                是否成功
                List<String> s = Arrays.asList(a[0].split("\\|"));
                if(ComicMake.make(
                        settings.sync_path+"/"+UUID.randomUUID().toString()+".CEIP",
                        s,s.get(Integer.parseInt(a[1])),
                        a[2],a[3],a[4],Arrays.asList(a[5].split("\\|")))){
                    sendText(out,"OK");
                    return;
                }
                sendText(out,"ERROR");
                return;
            }
            if(path.contains("/io/rdtmp")) {
                //title&anu&tip&ic&
                String[] a = URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8)).split("\\?");
                sendResponse(out,200, "OK","text/html",readBytes(new File(settings.tmp_path+"/tmp/"+a[1])));
                return;
            }
            if(path.contains("/sync")) {
                String r = "{\"list\":[";
                try {
                    List<String> filePaths = Files.walk(Paths.get(settings.sync_path))
                            .filter(Files::isRegularFile) // 过滤出文件，排除目录
                            .map(Path::toAbsolutePath) // 转换为绝对路径
                            .map(Path::toString) // 转换为字符串
                            .collect(Collectors.toList()); // 收集到列表中
//                    for (int i = 0; i < filePaths.size(); i++) {
//                        if (filePaths.get(i).contains(".zip") || filePaths.get(i).contains(".ZIP") && !(filePaths.get(i).contains(".CEIP") || filePaths.get(i).contains(".ceip"))){
//
//                            if(!new File(filePaths.get(i)+".CEIP").isFile()) {
//                                ComicMake.auto_make(filePaths.get(i) + ".CEIP", filePaths.get(i), settings.tmp_path);
//                            }
//                            //filePaths.add(filePaths.get(i)+".CEIP");
//                        }
//                    }
                    for (int i = 0; i < filePaths.size(); i++) {
                        if (filePaths.get(i).contains(".CEIP") || filePaths.get(i).contains(".ceip")){
                            r=r+"\""+filePaths.get(i)+"\",";
                        }
                    }
                    r=r.substring(0,r.length()-1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                r=r+"]}";
                r=r.replace("\\","\\\\");
                sendText(out,r);
                return;
            }
            if(path.contains("/read.html")) {
                sendResponse(out,200, "OK","text/html",readFileFromAssets(ct,"read.html"));
                return;
            }
            if(path.contains("/make.html")) {
                sendResponse(out,200, "OK","text/html",readFileFromAssets(ct,"make.html"));
                return;
            }
            if(path.contains("/imp")) {
                us = true;
                sendText(out,"<meta charset=\"UTF-8\">\n" +
                        "<div class=\"center-container\">\n" +
                        "    <img src=\"/bic.jpg\" alt=\"SYNCING...\" class=\"center-image\">\n" +
                        "    <p class=\"center-text\">正在导入文件...</p>\n" +
                        "</div>\n" +
                        "<style>\n" +
                        "    .center-container {\n" +
                        "        display: flex;\n" +
                        "        flex-direction: column; /* 设置为列布局 */\n" +
                        "        justify-content: center; /* 垂直居中 */\n" +
                        "        align-items: center; /* 水平居中（在列布局中，这实际上不会影响图片或文字的水平位置） */\n" +
                        "        height: 100%; /* 使得容器占据整个视口的高度 */\n" +
                        "        text-align: center; /* 使文字在图片下方居中 */\n" +
                        "        margin: 0; /* 去除任何可能的边距 */\n" +
                        "    }\n" +
                        "\n" +
                        "    .center-image {\n" +
                        "        width: 40%; /* 防止图片过大 */\n" +
                        "        height: auto; /* 保持图片的原始比例 */\n" +
                        "    }\n" +
                        "\n" +
                        "    .center-text {\n" +
                        "        margin-top: 10px; /* 根据需要调整图片和文字之间的间距 */\n" +
                        "    }\n" +
                        "</style>\n" +
                        "<script>\n" +
                        "    //var xhr = new XMLHttpRequest();\n" +
                        "    //xhr.open(\"GET\", \"/build\", true);\n" +
                        "    //xhr.onreadystatechange = function () {\n" +
                        "        //if (xhr.readyState === 4 && xhr.status === 200) {\n" +
                        "            window.location.href = \"/\"\n" +
                        "        //}\n" +
                        "   // }\n" +
                        "    //xhr.send();\n" +
                        "</script>");
                return;
            }
            if(path.contains("/bic.jpg")) {
                sendResponse(out,200, "OK","text/html",readFileFromAssets(ct,"load.gif"));
                return;
            }
            sendResponse(out,200, "OK","text/html",readFileFromAssets(ct,"index.html"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private byte[] readBytes(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream input = new FileInputStream(file)) {
            input.read(buffer);
        }
        return buffer;
    }
    public byte[] readFileFromAssets(Context context, String fileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        byte[] buffer = null;

        try {
            inputStream = assetManager.open(fileName);

            int size = inputStream.available();
            buffer = new byte[size];

            int readBytes = inputStream.read(buffer, 0, size);

            // 确保读取了所有字节
            if (readBytes < size) {
                throw new IOException("Could not read entire file");
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return buffer;
    }
    private void sendResponse(OutputStream out, int statusCode, String statusText, String contentType, byte[] data) {
        try{
            String statusLine = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n";
            String header = "Content-Type: " + contentType + "\r\n" + "Content-Length: " + data.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(statusLine.getBytes());
            out.write(header.getBytes());
            out.write(data);
        } catch (IOException e) {
            HTTP_SERVER.run_lock = false;
            if(HTTP_SERVER.debug) throw new RuntimeException(e);
        }
    }
    private void sendText(OutputStream out, String datas) throws IOException {
        byte[] data = datas.getBytes();
        sendResponse(out, 200, "OK", "text/html", data);
    }
}