package com.qcloud.cos.transfer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;

/**
 * Interface for multiple file transfers
 */
public abstract class MultipleFileTransfer<T extends Transfer> extends AbstractTransfer {

    protected final Collection<? extends T> subTransfers;

    /** Whether any of the sub-transfers has started. **/
    private AtomicBoolean subTransferStarted = new AtomicBoolean(false);

    MultipleFileTransfer(String description, TransferProgress transferProgress,
            ProgressListenerChain progressListenerChain, Collection<? extends T> subTransfers) {
        super(description, transferProgress, progressListenerChain);
        this.subTransfers = subTransfers;
    }

    /**
     * Set the state based on the states of all file downloads. Assumes all file
     * downloads are done.
     * <p>
     * A single failed sub-transfer makes the entire transfer failed. If there
     * are no failed sub-transfers, a single canceled sub-transfer makes the
     * entire transfer canceled. Otherwise, we consider ourselves Completed.
     */
    public void collateFinalState() {
        boolean seenCanceled = false;
        for ( T download : subTransfers ) {
            if ( download.getState() == TransferState.Failed ) {
                setState(TransferState.Failed);
                return;
            } else if ( download.getState() == TransferState.Canceled ) {
                seenCanceled = true;
            }
        }
        if ( seenCanceled )
            setState(TransferState.Canceled);
        else
            setState(TransferState.Completed);
    }

    /**
     * Override this method so that TransferState updates are also sent out to the
     * progress listener chain in forms of ProgressEvent.
     */
    @Override
    public void setState(TransferState state) {
        super.setState(state);

        switch (state) {
        case Waiting:
            fireProgressEvent(ProgressEventType.TRANSFER_PREPARING_EVENT);
            break;
        case InProgress:
            if ( subTransferStarted.compareAndSet(false, true) ) {
                /* The first InProgress signal */
                fireProgressEvent(ProgressEventType.TRANSFER_STARTED_EVENT);
            }
            /* Don't need any event code update for subsequent InProgress signals */
            break;
        case Completed:
            fireProgressEvent(ProgressEventType.TRANSFER_COMPLETED_EVENT);
            break;
        case Canceled:
            fireProgressEvent(ProgressEventType.TRANSFER_CANCELED_EVENT);
            break;
        case Failed:
            fireProgressEvent(ProgressEventType.TRANSFER_FAILED_EVENT);
            break;
        default:
            break;
        }
    }
}
