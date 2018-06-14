package com.qcloud.cos.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.LogFactory;

import com.qcloud.cos.exception.AbortedException;
import com.qcloud.cos.utils.IOUtils;

public abstract class SdkInputStream extends InputStream implements Releasable {
    /**
     * Returns the underlying input stream, if any, from the subclass; or null
     * if there is no underlying input stream.
     */
    abstract protected InputStream getWrappedInputStream();
    
    /**
    * Returns true if the current operation should abort; false otherwise.
    * Note the interrupted status of the thread is cleared by this method.
    */
   protected static boolean shouldAbort() {
       return Thread.interrupted();
   }
    
    /**
     * Aborts with subclass specific abortion logic executed if needed.
     * Note the interrupted status of the thread is cleared by this method.
     * @throws AbortedException if found necessary.
     */
    protected final void abortIfNeeded() {
        if (shouldAbort()) {
            try {
                abort();    // execute subclass specific abortion logic
            } catch (IOException e) {
                LogFactory.getLog(getClass()).debug("FYI", e);
            }
            throw new AbortedException();
        }
    }

    /**
     * Can be used to provide abortion logic prior to throwing the
     * AbortedException. No-op by default.
     */
    protected void abort() throws IOException {
        // no-op by default, but subclass such as COSObjectInputStream may override
    }

    /**
     * WARNING: Subclass that overrides this method must NOT call
     * super.release() or else it would lead to infinite loop.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void release() {
        // Don't call IOUtils.release(in, null) or else could lead to infinite loop
        IOUtils.closeQuietly(this, null);
        InputStream in = getWrappedInputStream();
        if (in instanceof Releasable) {
            // This allows any underlying stream that has the close operation
            // disabled to be truly released
            Releasable r = (Releasable)in;
            r.release();
        }
    }
}
