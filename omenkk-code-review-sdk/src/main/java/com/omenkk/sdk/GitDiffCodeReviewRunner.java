package com.omenkk.sdk;

import com.alibaba.fastjson2.JSON;
import com.omenkk.sdk.domain.model.ChatCompletionRequest;
import com.omenkk.sdk.domain.model.ChatCompletionSyncResponse;
import com.omenkk.sdk.domain.model.Model;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * @author omenkk7
 * @description git提交差异代码自动评审服务
 * @create 2025/10/15
 */
public class GitDiffCodeReviewRunner {

final static String GIT_PATH="https://github.com/Omenkk7/omenkk_code_review_log.git";



    public static void main(String[] args) throws Exception{
        //获取token
        String token =System.getenv("GITHUB_TOKEN");
        if(token==null||token.isEmpty()){
            throw new RuntimeException("token is empty");
        }
        //获取diff并且收集
        ProcessBuilder processBuilder=new ProcessBuilder("git", "diff", "HEAD~1","HEAD");
        processBuilder.directory(new File("."));//

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder diffCode=new StringBuilder();

        while((line= reader.readLine())!=null){
            diffCode.append(line);
        }

        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);

        System.out.println("diff code：" + diffCode.toString());

        // 2. 调用大模型 进行 代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code review：" + log);

        // 3. 写入日志
        writeLog()

    }

    //调用大模型进行代码自动评审
    private static String codeReview(String diffCode) throws IOException {

        //获取apikey解析获取token
 String apikeySecret="";
 String token="";


        //获取url
         URL url=new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        //创建并且填充并且发送http请求
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        ChatCompletionRequest chatCompletionRequest=new ChatCompletionRequest();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>(){
            private static final long serialVersionUID = -7988151926241837899L;
            {
                add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequest.Prompt("user", diffCode));
            }
        });
        OutputStream os = connection.getOutputStream();

        byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
        os.write(input);
        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content=new StringBuilder();
        while((inputLine=reader.readLine())!=null){
            content.append(inputLine);
        }
        //善后操作
        reader.close();
        connection.disconnect();
        System.out.println("评审结果"+content.toString());
        ChatCompletionSyncResponse  chatCompletionSyncResponse = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);

        //获取json格式后的数据
        return chatCompletionSyncResponse.getChoices().get(0).getMessage().getContent();

    }

    public static String writeLog(String token,String log) throws Exception{
        //拉取到本地
        Git git=Git.cloneRepository().setURI(GIT_PATH)
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token,""))
                .call();
        //创建当天的日志文件 如果没有的话
        String dateFormatName=new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder=new File("repo/"+dateFormatName);
        if(!dateFolder.exists()){
            //创建文件
            dateFolder.mkdirs();
        }
        //创建日志文件
        String fileName=generateRandomString(12)+".md";
        File newFile=new File(dateFolder,fileName);
        try(FileWriter fileWriter=new FileWriter(newFile)){
          fileWriter.write(log);
        }
        git.add().addFilepattern(dateFormatName+"/"+fileName).call();
        git.commit().setMessage("add new file via GitHub Actions").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token,"")).call();
        System.out.println("【log】change have been pushed to the repository");

        return "log";
    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
