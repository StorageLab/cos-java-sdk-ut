package com.qcloud.cos;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({BucketVersioningTest.class, BucketReplicationTest.class,
        CORSTest.class, CreateDeleteHeadBucketTest.class,
        GetBucketLocationTest.class, GetServiceTest.class, ListObjectTest.class, MultipartUploadTest.class,
        PutGetDelTest.class, PutObjectCopyTest.class})
    /** ignore AclTest.class, PutGetLifeCycleConfigTest.class**/
public class SuiteTest {
}
