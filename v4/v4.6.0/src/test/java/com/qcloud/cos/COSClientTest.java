package com.qcloud.cos;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.meta.InsertOnly;
import com.qcloud.cos.request.CreateFolderRequest;
import com.qcloud.cos.request.DelFileRequest;
import com.qcloud.cos.request.DelFolderRequest;
import com.qcloud.cos.request.GetFileLocalRequest;
import com.qcloud.cos.request.ListFolderRequest;
import com.qcloud.cos.request.StatFileRequest;
import com.qcloud.cos.request.StatFolderRequest;
import com.qcloud.cos.request.UpdateFileRequest;
import com.qcloud.cos.request.UpdateFolderRequest;
import com.qcloud.cos.request.UploadFileRequest;
import com.qcloud.cos.sign.Credentials;

public class COSClientTest {
    protected static String appid = null;
    protected static String secretId = null;
    protected static String secretKey = null;
    protected static String region = null;
    protected static String bucket = null;
    protected static ClientConfig clientConfig = null;
    protected static COSClient cosclient = null;
    protected static File tmpDir = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        destoryCosClient();
    }

    private File buildTestFile(long fileSize) throws IOException {
        String prefix = String.format("ut_size_%d_time_%d_", fileSize, System.currentTimeMillis());
        String suffix = ".tmp";
        File tmpFile = null;
        tmpFile = File.createTempFile(prefix, suffix, tmpDir);

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
            final int buffSize = 1024;
            byte[] tmpBuf = new byte[buffSize];
            long byteWriten = 0;
            while (byteWriten < fileSize) {
                ThreadLocalRandom.current().nextBytes(tmpBuf);
                long maxWriteLen = Math.min(buffSize, fileSize - byteWriten);
                bos.write(tmpBuf, 0, new Long(maxWriteLen).intValue());
                byteWriten += maxWriteLen;
            }
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
        return tmpFile;
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private static void initCosClient() throws Exception {
        appid = System.getenv("appid");
        secretId = System.getenv("secretId");
        secretKey = System.getenv("secretKey");
        region = System.getenv("region");
        bucket = System.getenv("bucket");

        File propFile = new File("ut_account.prop");
        if (propFile.exists() && propFile.canRead()) {
            Properties prop = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propFile);
                prop.load(fis);
                appid = prop.getProperty("appid");
                secretId = prop.getProperty("secretId");
                secretKey = prop.getProperty("secretKey");
                region = prop.getProperty("region");
                bucket = prop.getProperty("bucket");
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (secretId == null || secretKey == null || bucket == null || region == null) {
            System.out.println("cos ut user info missing. skip all test");
            return;
        }
        Credentials cred = new Credentials(Long.valueOf(appid), secretId, secretKey);
        clientConfig = new ClientConfig();
        clientConfig.setRegion(region);;
        cosclient = new COSClient(clientConfig, cred);
        tmpDir = new File("ut_test_tmp_data");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }

    private static void destoryCosClient() throws Exception {
        if (cosclient != null) {
            cosclient.shutdown();
        }
        if (tmpDir != null) {
            deleteDir(tmpDir);
        }
    }

    private boolean judgeUserInfoValid() {
        return cosclient != null;
    }

    // 从本地上传
    private void putObjectFromLocalFile(File localFile, String key, boolean enableSha) {
        if (!judgeUserInfoValid()) {
            return;
        }

        UploadFileRequest uploadFileRequest =
                new UploadFileRequest(bucket, key, localFile.getAbsolutePath());
        uploadFileRequest.setInsertOnly(InsertOnly.OVER_WRITE);
        uploadFileRequest.setEnableShaDigest(enableSha);

        String uploadRet = cosclient.uploadFile(uploadFileRequest);
        System.out.println(uploadRet);
        JSONObject uploadRetJson = new JSONObject(uploadRet);
        assertEquals(0, uploadRetJson.getInt("code"));
        assertTrue(uploadRetJson.has("request_id"));
        JSONObject dataJson = uploadRetJson.getJSONObject("data");
        assertTrue(dataJson.has("access_url"));
        assertTrue(dataJson.has("source_url"));

    }

    // 内存上传
    private void putFileFromContent(int uploadSize, String key, boolean enableSha) {
        if (!judgeUserInfoValid()) {
            return;
        }
        byte[] bytes = new byte[uploadSize];
        ThreadLocalRandom.current().nextBytes(bytes);

        UploadFileRequest uploadFileRequest = new UploadFileRequest(bucket, key, bytes);
        uploadFileRequest.setInsertOnly(InsertOnly.OVER_WRITE);
        uploadFileRequest.setEnableShaDigest(enableSha);

        String uploadRet = cosclient.uploadFile(uploadFileRequest);
        System.out.println(uploadRet);
        JSONObject uploadRetJson = new JSONObject(uploadRet);
        assertEquals(0, uploadRetJson.getInt("code"));
        assertTrue(uploadRetJson.has("request_id"));
        JSONObject dataJson = uploadRetJson.getJSONObject("data");
        assertTrue(dataJson.has("access_url"));
        assertTrue(dataJson.has("source_url"));
    }

    private void putFolder(String key) {
        CreateFolderRequest createFolderRequest = new CreateFolderRequest(bucket, key);
        String createFolderRet = cosclient.createFolder(createFolderRequest);
        JSONObject createFolderJson = new JSONObject(createFolderRet);
        assertEquals(0, createFolderJson.getInt("code"));
    }

    private void headFile(String key, long expectedLength, String bizAttr) {
        StatFileRequest statFileRequest = new StatFileRequest(bucket, key);
        String statRet = cosclient.statFile(statFileRequest);
        JSONObject statJson = new JSONObject(statRet);
        assertEquals(0, statJson.getInt("code"));
        JSONObject dataJson = statJson.getJSONObject("data");
        assertTrue(dataJson.has("access_url"));
        assertTrue(dataJson.has("source_url"));
        assertTrue(dataJson.has("biz_attr"));
        assertEquals(bizAttr, dataJson.get("biz_attr"));
        assertEquals(expectedLength, dataJson.getLong("filelen"));
        assertEquals(expectedLength, dataJson.getLong("filesize"));
    }

    private void headFolder(String key, String bizAttr) {
        StatFolderRequest statFolderRequest = new StatFolderRequest(bucket, key);
        String statFolderRet = cosclient.statFolder(statFolderRequest);
        JSONObject statJson = new JSONObject(statFolderRet);
        assertEquals(0, statJson.getInt("code"));
        JSONObject dataJson = statJson.getJSONObject("data");
        assertTrue(dataJson.has("biz_attr"));
        assertEquals(bizAttr, dataJson.get("biz_attr"));
    }


    // 下载COS的object
    private void getFile(String key, File localDownFile, long expectedLength) {
        GetFileLocalRequest getFileLocalRequest =
                new GetFileLocalRequest(bucket, key, localDownFile.getAbsolutePath());
        String getFileRet = cosclient.getFileLocal(getFileLocalRequest);
        JSONObject getFileRetJson = new JSONObject(getFileRet);
        assertEquals(0, getFileRetJson.getInt("code"));
        assertEquals(expectedLength, localDownFile.length());

    }

    private void updateFile(String key, String bizAttr) {
        UpdateFileRequest updateFileRequest = new UpdateFileRequest(bucket, key);
        updateFileRequest.setBizAttr(bizAttr);
        String updateFileRet = cosclient.updateFile(updateFileRequest);
        JSONObject updateJson = new JSONObject(updateFileRet);
        assertEquals(0, updateJson.getInt("code"));
    }

    private void updateFolder(String key, String bizAttr) {
        UpdateFolderRequest updateFolderRequest = new UpdateFolderRequest(bucket, key);
        updateFolderRequest.setBizAttr(bizAttr);
        String updateFolderRet = cosclient.updateFolder(updateFolderRequest);
        JSONObject updateJson = new JSONObject(updateFolderRet);
        assertEquals(0, updateJson.getInt("code"));
    }

    private void listFolder(String key) {
        ListFolderRequest listFolderRequest = new ListFolderRequest(bucket, key);
        String listFolderRet = cosclient.listFolder(listFolderRequest);
        JSONObject listFolderJson = new JSONObject(listFolderRet);
        assertEquals(0, listFolderJson.getInt("code"));
    }


    // 删除COS上的object
    protected void clearFile(String key) {
        if (!judgeUserInfoValid()) {
            return;
        }

        DelFileRequest delFileRequest = new DelFileRequest(bucket, key);
        String delRet = cosclient.delFile(delFileRequest);
        JSONObject delJson = new JSONObject(delRet);
        assertEquals(0L, delJson.getInt("code"));
    }

    // 删除COS上的object
    protected void clearFolder(String key) {
        if (!judgeUserInfoValid()) {
            return;
        }

        DelFolderRequest delFolderRequest = new DelFolderRequest(bucket, key);
        String delRet = cosclient.delFolder(delFolderRequest);
        JSONObject delJson = new JSONObject(delRet);
        assertEquals(0L, delJson.getInt("code"));
    }

    private void PutGetDelFileLocal(long fileSize, boolean enableSha) throws IOException {
        File localFile = buildTestFile(fileSize);
        File downloadFile = new File(localFile.getAbsolutePath() + ".down");
        try {
            String key = "/v4_ut/size_" + String.valueOf(fileSize) + ".txt";

            putObjectFromLocalFile(localFile, key, enableSha);

            String bizAttr = "ut_test:" + ThreadLocalRandom.current().nextLong();
            updateFile(key, bizAttr);
            headFile(key, fileSize, bizAttr);
            getFile(key, downloadFile, fileSize);
            clearFile(key);
        } catch (Exception e) {
            assertTrue(localFile.delete());
            assertTrue(downloadFile.delete());
        }
    }

    private void PutGetDelFileFromContent(long fileSize, boolean enableSha) throws IOException {
        String prefix = String.format("ut_size_%d_time_%d_", fileSize, System.currentTimeMillis());
        String suffix = ".tmp.down";
        File downloadFile = File.createTempFile(prefix, suffix, tmpDir);
        try {
            String key = "/v4_ut/size_" + String.valueOf(fileSize) + ".txt";
            putFileFromContent(new Long(fileSize).intValue(), key, enableSha);
            String bizAttr = "ut_test:" + ThreadLocalRandom.current().nextLong();
            updateFile(key, bizAttr);
            headFile(key, fileSize, bizAttr);
            getFile(key, downloadFile, fileSize);
            clearFile(key);
        } catch (Exception e) {
            assertTrue(downloadFile.delete());
        }
    }

    @Test
    public void testPutGetDelFile_size_0() throws IOException {
        PutGetDelFileLocal(0, true);
        PutGetDelFileLocal(0, false);
    }

    @Test
    public void testPutGetDelFile_size_1k() throws IOException {
        PutGetDelFileLocal(1024, true);
        PutGetDelFileLocal(1024, false);
    }

    @Test
    public void testPutGetDelFile_size_1M() throws IOException {
        PutGetDelFileLocal(1 * 1024 * 1024, true);
        PutGetDelFileLocal(1 * 1024 * 1024, true);
    }

    @Test
    public void testPutGetDelFile_size_10M() throws IOException {
        PutGetDelFileLocal(10 * 1024 * 1024, true);
        PutGetDelFileLocal(10 * 1024 * 1024, false);
    }

    @Test
    public void testPutGetDelFile_size_100M() throws IOException {
        PutGetDelFileLocal(100 * 1024 * 1024, true);
        PutGetDelFileLocal(100 * 1024 * 1024, false);
    }

    @Test
    public void testPutGetDelContent_size_0() throws IOException {
        PutGetDelFileFromContent(0, true);
        PutGetDelFileFromContent(0, false);
    }

    @Test
    public void testPutGetDelContent_size_1m() throws IOException {
        PutGetDelFileFromContent(1 * 1024 * 1024, true);
        PutGetDelFileFromContent(10 * 1024 * 1024, false);
    }

    @Test
    public void testPutGetDelContent_size_10m() throws IOException {
        PutGetDelFileFromContent(10 * 1024 * 1024, true);
        PutGetDelFileFromContent(10 * 1024 * 1024, false);
    }

    @Test
    public void testPutHeadDelListFolder() {
        String key = "/ut_folder/";
        putFolder(key);
        String bizAttr = "ut_test:" + ThreadLocalRandom.current().nextLong();
        updateFolder(key, bizAttr);
        listFolder(key);
        headFolder(key, bizAttr);
        clearFolder(key);
    }
}
