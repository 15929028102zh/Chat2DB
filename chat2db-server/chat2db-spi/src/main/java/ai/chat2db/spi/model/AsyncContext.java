package ai.chat2db.spi.model;

import ai.chat2db.server.tools.common.model.Context;
import ai.chat2db.server.tools.common.util.ContextUtils;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AsyncContext {

    private File writeFile;

    protected PrintWriter writer;

    protected boolean containsData;

    protected AsyncCall call;

    protected boolean finish;

    public File getWriteFile() {
        return writeFile;
    }


    public AsyncContext(AsyncCall call, Context context, File writeFile, boolean containsData) {
        this.call = call;
        this.writeFile = writeFile;
        this.progress = 5;
        this.containsData = containsData;
        createWriter();
        asyncCallBack(context);
    }

    private void createWriter() {
        if (writeFile != null) {
            this.writer = FileUtil.getPrintWriter(writeFile, "UTF-8", false);
        }
    }


    private void asyncCallBack(Context context) {
        if (call != null && context != null) {
            new Thread(() -> {
                try {
                    ContextUtils.setContext(context);
                    int n = 1;
                    while (!finish) {
                        // 更新时间逐渐变长避免频繁更新
                        Thread.sleep(2000 * n);
                        callUpdate();
                        if (n < 300) {
                            n++;
                        }
                    }
                } catch (Exception e) {
                    log.error("AsyncContext call error", e);
                } finally {
                    ContextUtils.removeContext();
                }
            }).start();
        }
    }

    private void callUpdate() {
        if (call == null) {
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("progress", progress);
        map.put("info", info.toString());
        map.put("error", error.toString());
        map.put("status", finish ? "FINISHED" : "RUNNING");
        if (progress == 100 && writeFile != null) {
            map.put("downloadUrl", writeFile.getAbsolutePath());
        }
        info = new StringBuffer();
        error = new StringBuffer();
        call.update(map);
    }

    public boolean isContainsData() {
        return containsData;
    }


    public void setProgress(Integer progress) {
        if (progress == null) {
            return;
        }
        this.progress = progress;
    }

    public void info(String message) {
        info.append(message + "\n");
    }

    public void error(String message) {
        error.append(message + "\n");
        info.append(message + "\n");
    }

    public void finish() {
        finish = true;
        this.progress = 100;
        if (writer != null) {
            writer.flush();
            writer.close();
        }
        callUpdate();

    }

    public void write(String message) {
        if (writer != null) {
            writer.write(message + "\n");
        }
    }

    protected Integer progress;

    private StringBuffer info = new StringBuffer();

    private StringBuffer error = new StringBuffer();


}
