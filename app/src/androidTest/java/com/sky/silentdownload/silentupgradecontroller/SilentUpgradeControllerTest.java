package com.sky.silentdownload.silentupgradecontroller;

import android.content.Context;
import android.test.AndroidTestCase;

import com.sky.silentdownload.silentupgrade.downloader.DownloadManager;
import com.sky.silentdownload.silentupgrade.downloader.data.ApkDownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;
import com.sky.silentdownload.silentupgrade.installer.IInstallerListener;
import com.sky.silentdownload.silentupgrade.installer.impl.InstallerImpl;
import com.sky.silentdownload.silentupgrade.silentupgradecontroller.SilentUpgradeController;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BaoCheng on 2017/3/7.
 */
public class SilentUpgradeControllerTest extends AndroidTestCase {

    private Context mContext;

    private SilentUpgradeController controller = null;

    private DownloadManager downloadManager = null;

    private InstallerImpl installer = null;

    private ApkDownloadInfo info = null;

    private List<DownloadInfo> list = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        controller = new SilentUpgradeController();
        downloadManager = Mockito.mock(DownloadManager.class);
        installer = Mockito.mock(InstallerImpl.class);
        info = new ApkDownloadInfo();
        info.url = "123";
        info.md5 = "456";
        info.map.put("apk_version", "1.01");
        info.map.put("download_state", "1003");
        info.setApk("com.example.test");

        list = new ArrayList<>();

        list.add(info);
    }

    /**
     * 测试距上次获取静默升级数据小于8小时，走的是小于8小时的判断分支
     */
    @Test
    public void testRequestDataFromLastLessThan8Hours() throws Exception {

        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        Mockito.when(mockController.calculateTime(Mockito.anyLong(), Mockito.anyLong())).thenReturn(1000l);

        mockController.init(mContext);

        assertEquals(mockController.tempTime, ((8 * 60 * 60 * 1000) - 1000));
    }

    /**
     * 测试距上次请求静默升级数据大于8小时，直接请求了静默升级数据 requestSilentUpgradeData()得到了调用
     */
    @Test
    public void testRequestDataFromLastMoreThan8Hours() {
        SilentUpgradeController controller = Mockito.spy(SilentUpgradeController.class);
        Mockito.when(controller.calculateTime(Mockito.anyLong(), Mockito.anyLong())).thenReturn((8 * 60 * 60 * 1000) * 2l);

        controller.init(mContext);
        Mockito.verify(controller).requestSilentUpgradeData();
    }

    /**
     * 测试下载完成，调用安装器安装
     */
    @Test
    public void testDownloadSuccessAndInstall() {
        controller.setDownloadManager(downloadManager);
        controller.setInstaller(installer);

        Mockito.when(downloadManager.getNativePath(Mockito.any(DownloadInfo.class))).thenReturn("path");
        controller.onDownloadStatus(Status.FINISH, info, null);

        Mockito.verify(installer).install("path");
    }

    /**
     * 测试下载失败，调用下载器删除
     */
    @Test
    public void testDownloadFailedAndRemove() {
        controller.setDownloadManager(downloadManager);
        controller.onDownloadStatus(Status.ERROR, info, null);
        Mockito.verify(downloadManager).remove(Mockito.any(DownloadInfo.class));
    }

    /**
     * 测试开始静默下载时,获取了静默下载任务列表
     */
    @Test
    public void testGetDownloadStatus() {
        controller.setDownloadManager(downloadManager);
        controller.setDownloadManager(downloadManager);

        controller.startSilentDownload();

        Mockito.verify(downloadManager).list();
    }

    /**
     * 测试下载失败，删除了静默下载任务
     */
    @Test
    public void testDownloadFail() {
        controller.setDownloadManager(downloadManager);
        controller.onDownloadStatus(Status.ERROR, info, null);

        Mockito.verify(downloadManager).remove(info);
    }

    /**
     * 测试下载成功，获取了 apk 本地路径
     */
    @Test
    public void testDownloadSuccess() {
        controller.setDownloadManager(downloadManager);
        controller.setInstaller(installer);
        controller.onDownloadStatus(Status.FINISH, info, null);

        Mockito.verify(downloadManager).getNativePath(info);
    }

    /**
     * 测试安装成功，走进了安装成功回调，并删除了安装包
     */
    @Test
    public void testInstallSuccess() {
        controller.setDownloadManager(downloadManager);
        controller.onInstallListener(IInstallerListener.INSTALL_SUCCESS, "com.example.test", null);

        installer.setInstallListener(new IInstallerListener() {
            @Override
            public void onInstallListener(int result, String pkg, String extra) {
                assertEquals(result, IInstallerListener.INSTALL_SUCCESS);
                DownloadInfo info = controller.getDownloadInfoByPackageName(pkg);
                assertNotNull(info);
                Mockito.verify(downloadManager).remove(info);
            }
        });
    }

    /**
     * 测试距上次请求静默升级数据小于8小时，进入了小于8小时判断分支，并且获取到了静默下载任务
     */
    @Test
    public void testSilentDownloadAndSilentInstall() {
        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        Mockito.when(mockController.calculateTime(Mockito.anyLong(), Mockito.anyLong())).thenReturn(5000l);
        mockController.init(mContext);
        List<DownloadInfo> downloadInfos = mockController.requestSilentDownloadTask();
        assertEquals(mockController.tempTime, (8 * 60 * 60 * 1000) - 5000);
        assertNotNull(downloadInfos);
    }


    /**
     * 测试无静默升级数据，有静默下载任务，是否直接删除了所有静默下载任务
     */
    @Test
    public void testNoUpgradeDataAndHasSilentDownloadTask() {
        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        mockController.setDownloadManager(downloadManager);
        Mockito.when(mockController.requestSilentUpgradeData()).thenReturn(null);
        Mockito.when(mockController.requestSilentDownloadTask()).thenReturn(list);
        mockController.init(mContext);
        mockController.startDownloadAfterGetUpgradeData();
        Mockito.verify(downloadManager).remove(info);
    }

    /**
     * 测试有静默升级数据，无静默下载任务，是否创建了静默下载任务
     */
    @Test
    public void testHasUpgradeDataAndNoSilentDownloadTask() throws InterruptedException {
        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        mockController.setDownloadManager(downloadManager);
        Mockito.when(mockController.requestSilentUpgradeData()).thenReturn(list);
        Mockito.when(mockController.requestSilentDownloadTask()).thenReturn(null);
        mockController.init(mContext);
        mockController.startDownloadAfterGetUpgradeData();

       Mockito.verify(downloadManager).download(Mockito.any(DownloadInfo.class));
    }

    /**
     * 测试有静默升级数据，对应应用下载url相同，下载任务未完成，是否调用了静默下载
     */
    @Test
    public void testHasUpgradeDataAndDownloadTaskIsNotFinish() {
        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        Mockito.when(mockController.requestSilentUpgradeData()).thenReturn(list);
        Mockito.when(mockController.requestSilentDownloadTask()).thenReturn(list);
        mockController.setDownloadManager(downloadManager);
        mockController.init(mContext);
        mockController.startDownloadAfterGetUpgradeData();

        Mockito.verify(downloadManager).download(info);
    }

    /**
     * 测试有静默升级数据，对应应用下载url相同，下载任务已完成，是否调用了静默安装
     */
    @Test
    public void testHasUpgradeDataAndDownloadTaskIsFinished() {
        ApkDownloadInfo info1 = new ApkDownloadInfo();
        info1.url = "123";
        info1.md5 = "456";
        info1.map.put("apk_version", "1.01");
        info1.map.put("download_state", "1005");
        info1.setApk("com.example.test");

        List<DownloadInfo> list1 = new ArrayList<>();
        list1.add(info1);

        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        mockController.setDownloadManager(downloadManager);
        Mockito.when(downloadManager.list()).thenReturn(list1);
        Mockito.when(downloadManager.getDownloadStatus(Mockito.any(DownloadInfo.class))).thenReturn(Status.FINISH);
        Mockito.when(downloadManager.getNativePath(Mockito.any(DownloadInfo.class))).thenReturn("sdcard");
        Mockito.when(mockController.requestSilentUpgradeData()).thenReturn(list);
        Mockito.when(mockController.requestSilentDownloadTask()).thenReturn(list1);
        mockController.setInstaller(installer);
        mockController.init(mContext);
        mockController.startDownloadAfterGetUpgradeData();

        Mockito.verify(mockController,Mockito.atLeast(1)).installPackage(Mockito.anyString());
    }

    /**
     * 测试有静默升级数据，对应应用下载url不相同，是否删除该对应下载任务，创建新的静默下载任务
     */
    @Test
    public void testHasUpgradeDataAndDownloadUrlIsNotSame() {
        List<DownloadInfo> list1 = new ArrayList<>();
        ApkDownloadInfo info1 = new ApkDownloadInfo();
        info1.url = "456";
        info1.md5 = "";
        info1.map.put("apk_version", "1.01");
        info1.map.put("download_state", "1003");
        info1.setApk("com.example.test");
        list1.add(info1);

        SilentUpgradeController mockController = Mockito.spy(SilentUpgradeController.class);
        mockController.setDownloadManager(downloadManager);
        Mockito.when(downloadManager.list()).thenReturn(list1);
        Mockito.when(downloadManager.getNativePath(Mockito.any(DownloadInfo.class))).thenReturn("sdcard");
        Mockito.when(mockController.requestSilentUpgradeData()).thenReturn(list);
        Mockito.when(mockController.requestSilentDownloadTask()).thenReturn(list1);
        mockController.setInstaller(installer);
        mockController.init(mContext);
        mockController.startDownloadAfterGetUpgradeData();

        Mockito.verify(mockController).removeSilentDownloadTask(info1);
        Mockito.verify(downloadManager).download(info);
    }
}
