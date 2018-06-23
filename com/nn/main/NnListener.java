package com.nn.main;

/**
 * 工作进度等的监听
 */
public interface NnListener {
    void progress(double progress);// 工作进度

    void errorInfo(String info);// 错误信息（在工作线程出错，要通知用户）

    void complete();// 工作完成
}
