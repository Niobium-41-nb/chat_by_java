import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteLog {
    public static void write(String f, String content) {
        File file = new File(f);
        try (FileWriter fileWriter = new FileWriter(file, true)) { // 第二个参数设为true启用追加模式
            // 获取当前时间并格式化
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            // 写入带时间戳的内容，每行一个日志记录
            fileWriter.append(timestamp + " " + content + "\n");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}