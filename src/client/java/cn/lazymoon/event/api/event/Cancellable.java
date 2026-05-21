package cn.lazymoon.event.api.event;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public interface Cancellable {

    /**
     * Checks if the object is cancelled.
     *
     * @return {@code true} if the object is cancelled, {@code false} otherwise.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of the object.
     *
     * @param state {@code true} to cancel the object, {@code false} to uncancel it.
     */
    void setCancelled(boolean state);
}