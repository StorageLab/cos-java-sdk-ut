package com.qcloud.cos;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({BucketVersioningTest.class, BucketReplicationTest.class,
        CORSTest.class, CreateDeleteHeadBucketTest.class,
        GetBucketLocationTest.class, GetServiceTest.class, ListObjectTest.class, MultipartUploadTest.class,
        PutGetDelTest.class, PutGetLifeCycleConfigTest.class, PutObjectCopyTest.class})
    /** ignore AclTest.class**/
public class SuiteTest {
}
