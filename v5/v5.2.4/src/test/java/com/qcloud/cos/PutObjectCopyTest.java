package com.qcloud.cos;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.utils.Md5Utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PutObjectCopyTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    private void testCopySameRegionDiffSize(long fileSize, ObjectMetadata newObjectMetaData) throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(fileSize);
        String srcEtag = Md5Utils.md5Hex(localFile);

        String srcKey = String.format("ut/src_len_%d.txt", fileSize);
        String destKey = String.format("ut/dest_len_%d.txt", fileSize);
        try {
            PutObjectResult putObjectResult = putObjectFromLocalFile(localFile, srcKey);
            CopyObjectRequest copyObjectRequest =
                    new CopyObjectRequest(bucket, srcKey, bucket, destKey);
            copyObjectRequest.setSourceVersionId(putObjectResult.getVersionId());
            copyObjectRequest.setStorageClass(StorageClass.Standard_IA);
            if (newObjectMetaData != null) {
                copyObjectRequest.setNewObjectMetadata(newObjectMetaData);
            }
            CopyObjectResult copyObjectResult = cosclient.copyObject(copyObjectRequest);
            assertEquals(srcEtag, copyObjectResult.getETag());
            headSimpleObject(srcKey, fileSize, srcEtag);
            ObjectMetadata destObjectMetadata = headSimpleObject(destKey, fileSize, srcEtag);
            if (newObjectMetaData != null) {
                checkMetaData(newObjectMetaData, destObjectMetadata);
            }
            
        } finally {
            // delete file on cos
            clearObject(srcKey);
            clearObject(destKey);
            // delete local file
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
        }
    }

    @Test
    public void testCopySameRegionEmpty() throws IOException {
        testCopySameRegionDiffSize(0L, null);
    }

    @Test
    public void testCopySameRegion1M() throws IOException {
        testCopySameRegionDiffSize(1 * 1024 * 1024L, null);
    }
    
    @Test
    public void testCopySameRegion10M() throws IOException {
        testCopySameRegionDiffSize(10 * 1024 * 1024L, null);
    }
    
    @Test
    public void testCopySameRegionEmptyWithNewMetaData() throws IOException {
        ObjectMetadata newObjectMetadata = new ObjectMetadata();
        newObjectMetadata.setServerSideEncryption("AES256");
        newObjectMetadata.setContentType("image/tiff");
        testCopySameRegionDiffSize(0, newObjectMetadata);
    }
    
    @Test
    public void testCopySameRegion10MWithNewMetaData() throws IOException {
        ObjectMetadata newObjectMetadata = new ObjectMetadata();
        newObjectMetadata.setServerSideEncryption("AES256");
        newObjectMetadata.setContentType("image/png");
        newObjectMetadata.setCacheControl("no-cache");
        testCopySameRegionDiffSize(10 * 1024 * 1024L, newObjectMetadata);
    }
    
    @Test
    public void testDeleteNotExistObject() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        String key = "ut/not-exist.txt";
        try {
            cosclient.deleteObject(bucket, key);
        } catch (CosServiceException e) {
            fail(e.toString());
        }
    }
}
