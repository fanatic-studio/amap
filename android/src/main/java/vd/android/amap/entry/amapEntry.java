package vd.android.amap.entry;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import app.vd.framework.extend.annotation.ModuleEntry;
import vd.android.amap.component.WXMapCircleComponent;
import vd.android.amap.component.WXMapInfoWindowComponent;
import vd.android.amap.component.WXMapMarkerComponent;
import vd.android.amap.component.WXMapPolyLineComponent;
import vd.android.amap.component.WXMapPolygonComponent;
import vd.android.amap.component.WXMapViewComponent;
import vd.android.amap.module.WXMapModule;

@ModuleEntry
public class amapEntry {

    /**
     * APP启动会运行此函数方法
     * @param content Application
     */
    public void init(Context content) {

        //1、注册weex模块
        try {
            WXSDKEngine.registerComponent("vd-amap-circle", WXMapCircleComponent.class);
            WXSDKEngine.registerComponent("vd-amap-info-window", WXMapInfoWindowComponent.class);
            WXSDKEngine.registerComponent("vd-amap-marker", WXMapMarkerComponent.class);
            WXSDKEngine.registerComponent("vd-amap-polygon", WXMapPolygonComponent.class);
            WXSDKEngine.registerComponent("vd-amap-polyline", WXMapPolyLineComponent.class);
            WXSDKEngine.registerComponent("vd-amap", WXMapViewComponent.class);
            WXSDKEngine.registerModule("vdAmap", WXMapModule.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }

}
