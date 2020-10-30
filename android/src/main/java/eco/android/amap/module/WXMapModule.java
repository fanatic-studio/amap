package eco.android.amap.module;

import androidx.annotation.Nullable;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.utils.WXLogUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.eco.framework.extend.module.utilcode.constant.PermissionConstants;
import app.eco.framework.extend.module.utilcode.util.PermissionUtils;
import app.eco.framework.extend.module.ecoBase;
import app.eco.framework.extend.module.ecoJson;
import eco.android.amap.bean.PoiSearchBean;
import eco.android.amap.component.WXMapPolygonComponent;


/**
 * Created by budao on 2017/1/24.
 */

public class WXMapModule extends WXModule {
    private static final String RESULT = "result";
    private static final String DATA = "data";

    private static final String RESULT_OK = "success";
    private static final String RESULT_FAILED = "failed";

    /**
     * get line distance between to POI.
     */
    @JSMethod
    public void getLineDistance(String posA, String posB, @Nullable final JSCallback callback) {
        Log.v("getDistance", posA + ", " + posB);
        float distance = -1;
        try {
            JSONArray jsonArray = new JSONArray(posA);
            LatLng latLngA = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));
            JSONArray jsonArrayB = new JSONArray(posB);
            LatLng latLngB = new LatLng(jsonArrayB.optDouble(1), jsonArrayB.optDouble(0));
            distance = AMapUtils.calculateLineDistance(latLngA, latLngB);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (callback != null) {
            HashMap map = new HashMap(2);
            HashMap data = new HashMap(1);
            data.put("distance", distance);
            map.put(DATA, data);
            map.put(RESULT, distance >= 0 ? RESULT_OK : RESULT_FAILED);
            callback.invoke(map);
        }

    }

    @JSMethod
    public void polygonContainsMarker(String position, String id, @Nullable final JSCallback callback) {
        boolean contains = false;
        boolean success = false;
        try {
            JSONArray jsonArray = new JSONArray(position);
            LatLng latLng = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));

            WXComponent component = findComponent(id);

            if (component != null && component instanceof WXMapPolygonComponent) {
                contains = ((WXMapPolygonComponent) component).contains(latLng);
                success = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (callback != null) {
            Map<String, Object> map = new HashMap<>();
            map.put(DATA, contains);
            map.put(RESULT, success ? RESULT_OK : RESULT_FAILED);
            callback.invoke(map);
        }
    }

    private void startLocation(String id, @Nullable final JSCallback callback) {
        final AMapLocationClient client = new AMapLocationClient(
                WXEnvironment.getApplication().getApplicationContext());
        final AMapLocationClientOption clientOption = new AMapLocationClientOption();
        //设置定位监听
        client.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                    if (callback != null) {
                        HashMap map = new HashMap(2);
                        HashMap data = new HashMap(1);
                        ArrayList position = new ArrayList();
                        position.add(aMapLocation.getLongitude());
                        position.add(aMapLocation.getLatitude());
                        data.put("position", position);
                        data.put("addressName", aMapLocation.getAoiName());
                        data.put("address", aMapLocation.getAddress());
                        data.put("country", aMapLocation.getCountry());
                        data.put("province", aMapLocation.getProvince());
                        data.put("city", aMapLocation.getCity());
                        data.put("cityCode", aMapLocation.getCityCode());
                        data.put("district", aMapLocation.getDistrict());
                        data.put("adCode", aMapLocation.getAdCode());
                        data.put("street", aMapLocation.getStreet());
                        data.put("streetNum", aMapLocation.getStreetNum());
                        data.put("pioName", aMapLocation.getPoiName());
                        map.put(DATA, data);
                        map.put(RESULT, aMapLocation.getLongitude() > 0 && aMapLocation.getLatitude() > 0 ? RESULT_OK : RESULT_FAILED);
                        callback.invokeAndKeepAlive(map);
                    }
                } else {
                    String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                    WXLogUtils.e("WXMapModule", errText);
                }
                if (client != null) {
                    client.stopLocation();
                    client.onDestroy();
                }
            }
        });
        //设置为高精度定位模式
        clientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        clientOption.setOnceLocation(true);
        //设置定位参数
        client.setLocationOption(clientOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        client.startLocation();
    }

    /**
     * get user location.
     */
    @JSMethod
    public void getUserLocation(String id, @Nullable final JSCallback callback) {
        Context mContext = mWXSDKInstance.getContext();
        PermissionUtils.permission(PermissionConstants.LOCATION)
                .rationale(shouldRequest -> PermissionUtils.showRationaleDialog(mContext, shouldRequest, "获取定位"))
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                      startLocation(id, callback);
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        if (!permissionsDeniedForever.isEmpty()) {
                            PermissionUtils.showOpenAppSettingDialog(mContext, "获取定位");
                        }
                    }
                }).request();
    }

    /**
     * 搜索poi
     * @param keywords  关键字
     * @param city 城市
     * @param types 搜索类型
     */
    private void startPoiSearch(String keywords, String city, String types, @Nullable final JSCallback callback) {
        InputtipsQuery inputquery = new InputtipsQuery(keywords,city);
        inputquery.setCityLimit(true);//限制在当前城市
        Inputtips inputTips = new Inputtips(WXEnvironment.getApplication().getApplicationContext(), inputquery);
        List<PoiSearchBean> pois = new ArrayList();
        inputTips.setInputtipsListener(new Inputtips.InputtipsListener() {
            @Override
            public void onGetInputtips(List<Tip> list, int i) {
                if (list.size() > 0) {
                    for (int a = 0; a < list.size(); a++) {
                      List<String> locations=new ArrayList<>();
                      locations.add(String.valueOf(list.get(a).getPoint().getLatitude()));
                      locations.add(String.valueOf(list.get(a).getPoint().getLongitude()));
                        PoiSearchBean data = new PoiSearchBean(list.get(a).getPoiID(),
                                String.valueOf(list.get(a).getPoint().getLatitude()),
                                String.valueOf(list.get(a).getPoint().getLongitude()),
                                locations,
                                list.get(a).getName(),
                                list.get(a).getAddress());
                        pois.add(data);
                    }
                }
                callback.invokeAndKeepAlive(pois);
            }
        });
        inputTips.requestInputtipsAsyn();
    }


    @JSMethod
    public void poiSearch(String keywords,  @Nullable final JSCallback callback) {
      JSONObject object = JSONObject.parseObject(keywords);
      startPoiSearch(object.getString("keywords"),object.getString("city"),object.getString("types") , callback);
    }


    @JSMethod
    public void init() {
        JSONObject umeng = ecoJson.parseObject(ecoBase.config.getObject("amap").get("android"));
        String appKey = umeng.getString("appKey");
        MapsInitializer.setApiKey(appKey);
        AMapLocationClient.setApiKey(appKey);
    }
}
