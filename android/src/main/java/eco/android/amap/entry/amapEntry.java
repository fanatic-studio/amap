package eco.android.amap.entry;

import android.content.Context;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import app.eco.framework.extend.annotation.ModuleEntry;
import eco.android.amap.component.WXMapCircleComponent;
import eco.android.amap.component.WXMapInfoWindowComponent;
import eco.android.amap.component.WXMapMarkerComponent;
import eco.android.amap.component.WXMapPolyLineComponent;
import eco.android.amap.component.WXMapPolygonComponent;
import eco.android.amap.component.WXMapViewComponent;
import eco.android.amap.module.WXMapModule;

@ModuleEntry
public class amapEntry {

    /**
     * APP启动会运行此函数方法
     * @param content Application
     */
    public void init(Context content) {

        //1、注册weex模块
        try {
            WXSDKEngine.registerComponent("eco-amap-circle", WXMapCircleComponent.class);
            WXSDKEngine.registerComponent("eco-amap-info-window", WXMapInfoWindowComponent.class);
            WXSDKEngine.registerComponent("eco-amap-marker", WXMapMarkerComponent.class);
            WXSDKEngine.registerComponent("eco-amap-polygon", WXMapPolygonComponent.class);
            WXSDKEngine.registerComponent("eco-amap-polyline", WXMapPolyLineComponent.class);
            WXSDKEngine.registerComponent("eco-amap", WXMapViewComponent.class);
            WXSDKEngine.registerModule("ecoAmap", WXMapModule.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
    }

}
