//
//  WXMapViewModule.m
//  Pods
//
//  Created by yangshengtao on 17/1/23.
//
//

#import "WXMapViewModule.h"
#import "WXMapViewComponent.h"
#import "WXConvert+AMapKit.h"
#import <WeexPluginLoader/WeexPluginLoader.h>

WX_PlUGIN_EXPORT_MODULE(ecoAmap, WXMapViewModule)

@interface WXMapViewModule () <AMapSearchDelegate>

@property (nonatomic, strong) MAMapView *mapView;

@property (nonatomic, strong) AMapSearchAPI *search;

@property (nonatomic, copy) WXModuleCallback callback;

@property (nonatomic, strong) AMapLocationManager *locationManager;

@end

@implementation WXMapViewModule

@synthesize weexInstance;

WX_EXPORT_METHOD(@selector(getUserLocation:callback:))
WX_EXPORT_METHOD(@selector(getLineDistance:marker:callback:))
WX_EXPORT_METHOD_SYNC(@selector(polygonContainsMarker:ref:callback:))
WX_EXPORT_METHOD(@selector(getAroundLocation:callback:))
WX_EXPORT_METHOD(@selector(poiSearch:callback:))

- (void)getUserLocation:(NSString *)elemRef callback:(WXModuleCallback)callback
{
    if (elemRef.length == 0) {
        [self getAroundLocation:elemRef callback:callback];
        return;
    }
    [self performBlockWithRef:elemRef block:^(WXComponent *component) {
        callback([(WXMapViewComponent *)component getUserLocation] ? : nil);
    }];
}

- (void)getLineDistance:(NSArray *)marker marker:(NSArray *)anotherMarker callback:(WXModuleCallback)callback
{
    CLLocationCoordinate2D location1 = [WXConvert CLLocationCoordinate2D:marker];
    CLLocationCoordinate2D location2 = [WXConvert CLLocationCoordinate2D:anotherMarker];
    MAMapPoint p1 = MAMapPointForCoordinate(location1);
    MAMapPoint p2 = MAMapPointForCoordinate(location2);
    CLLocationDistance distance =  MAMetersBetweenMapPoints(p1, p2);
    NSDictionary *userDic;
    if (distance > 0) {
        userDic = @{@"result":@"success",@"data":@{@"distance":[NSNumber numberWithDouble:distance]}};
    }else {
        userDic = @{@"resuldt":@"false",@"data":@""};
    }
    callback(userDic);
}

- (void)getAroundLocation:(NSString *)ref callback:(WXModuleCallback)callback {
    [self.locationManager requestLocationWithReGeocode:YES completionBlock:^(CLLocation *location, AMapLocationReGeocode *formattedAddress, NSError *error) {
        NSDictionary *userDic;

        if (error) {
            userDic = @{@"resuldt":@"false",@"data":@{@"errorCode": @(error.code), @"description": error.description}};
        } else {
            NSArray *coordinate = @[[NSNumber numberWithDouble:location.coordinate.longitude],[NSNumber numberWithDouble:location.coordinate.latitude]];
            userDic = @{@"result":@"success",@"data":@{
                                @"position":coordinate,
                                @"title":@"",
                                @"addressName":formattedAddress.AOIName ? formattedAddress.AOIName : @"",
                                @"address":formattedAddress.formattedAddress ? formattedAddress.formattedAddress : @"",
                                @"country":formattedAddress.country ? formattedAddress.country : @"",
                                @"province":formattedAddress.province ? formattedAddress.province : @"",
                                @"city":formattedAddress.city ? formattedAddress.city : @"",
                                @"citycode":formattedAddress.citycode ? formattedAddress.citycode : @"",
                                @"district":formattedAddress.district ? formattedAddress.district : @"",
                                @"adcode":formattedAddress.adcode ? formattedAddress.adcode : @"",
                                @"street":formattedAddress.street ? formattedAddress.street : @"",
                                @"streetNum":formattedAddress.number ? formattedAddress.number : @"",
                                @"POIName":formattedAddress.POIName ? formattedAddress.POIName : @"",
            }};
        }
        callback(userDic);
    }];
}

- (AMapLocationManager *)locationManager {
    if (_locationManager == nil) {
        _locationManager = [[AMapLocationManager alloc] init];
        [_locationManager setDesiredAccuracy:kCLLocationAccuracyHundredMeters];
        _locationManager.locationTimeout = 2;
        _locationManager.reGeocodeTimeout = 10;
    }
    return _locationManager;
}

- (void)poiSearch:(NSDictionary *)params callback:(WXModuleCallback)callback {
    if (self.search == nil) {
        self.search = [[AMapSearchAPI alloc] init];
        self.search.delegate = self;
    }
    self.callback = callback;
    NSString *keywords = [params valueForKey:@"keywords"];
    NSString *city = [params valueForKey:@"city"];
    NSString *types = [params valueForKey:@"types"];

    // 关键字检索
    AMapPOIKeywordsSearchRequest *request = [[AMapPOIKeywordsSearchRequest alloc] init];
    request.keywords = keywords;
    request.city = city;
    request.types = types;
    request.requireExtension = YES;
    request.cityLimit = YES;
    request.requireSubPOIs = YES;
    request.offset = 10;
    
    [self.search AMapPOIKeywordsSearch:request];
}

// 逆地理位置编码结果
- (void)onReGeocodeSearchDone:(AMapReGeocodeSearchRequest *)request response:(AMapReGeocodeSearchResponse *)response {
    NSLog(@"%@", response);
    NSString *address = @"";
    if (response.regeocode.aois.count > 0) {
        address = [response.regeocode.aois[0] valueForKey:@"name"];
    }
    MAUserLocation *location = self.mapView.userLocation;
    NSDictionary *userDic;
    NSArray *coordinate = @[[NSNumber numberWithDouble:location.location.coordinate.longitude],[NSNumber numberWithDouble:location.location.coordinate.latitude]];
    userDic = @{@"result":@"success",@"data":@{@"position":coordinate,@"title":@"",@"address":address}};
    self.callback(userDic);
}

// 关键字检索结果
- (void)onPOISearchDone:(AMapPOISearchBaseRequest *)request response:(AMapPOISearchResponse *)response {
    NSMutableArray *result = [NSMutableArray array];
    for (AMapPOI *poi in response.pois) {
        NSMutableDictionary *dict = [NSMutableDictionary dictionary];
        NSLog(@"%@--%@", poi.address, poi.name);
        [dict setValue:poi.uid forKey:@"id"];
        [dict setValue:@(poi.location.latitude) forKey:@"latitude"];
        [dict setValue:@(poi.location.longitude) forKey:@"longitude"];
        [dict setValue:[NSString stringWithFormat:@"%@,%@", @(poi.location.longitude), @(poi.location.latitude)] forKey:@"location"];
        [dict setValue:poi.name forKey:@"name"];
        [dict setValue:poi.address forKey:@"address"];
        [result addObject:dict];
    }
    self.callback(result);
}

// 附近位置检索结果
- (void)onNearbySearchDone:(AMapNearbySearchRequest *)request response:(AMapNearbySearchResponse *)response {
    NSLog(@"%@", request);
}

- (void)AMapSearchRequest:(id)request didFailWithError:(NSError *)error {
    NSLog(@"%@", error);
    NSDictionary *userDic;
    userDic = @{@"resuldt":@"false",@"data":error.description};
    self.callback(userDic);
}

- (void)polygonContainsMarker:(NSArray *)position ref:(NSString *)elemRef callback:(WXModuleCallback)callback
{
    [self performBlockWithRef:elemRef block:^(WXComponent *WXMapRenderer) {
        CLLocationCoordinate2D loc1 = [WXConvert CLLocationCoordinate2D:position];
        MAMapPoint p1 = MAMapPointForCoordinate(loc1);
        NSDictionary *userDic;

        if (![WXMapRenderer.shape isKindOfClass:[MAMultiPoint class]]) {
            userDic = @{@"result":@"false",@"data":[NSNumber numberWithBool:NO]};
            return;
        }
        MAMapPoint *points = ((MAMultiPoint *)WXMapRenderer.shape).points;
        NSUInteger pointCount = ((MAMultiPoint *)WXMapRenderer.shape).pointCount;
        
        if(MAPolygonContainsPoint(p1, points, pointCount)) {
             userDic = @{@"result":@"success",@"data":[NSNumber numberWithBool:YES]};
        } else {
            userDic = @{@"result":@"false",@"data":[NSNumber numberWithBool:NO]};
        }
        callback(userDic);
    }];
}

- (void)performBlockWithRef:(NSString *)elemRef block:(void (^)(WXComponent *))block {
    if (!elemRef) {
        return;
    }
    
    __weak typeof(self) weakSelf = self;
    
    WXPerformBlockOnComponentThread(^{
        WXComponent *component = (WXComponent *)[weakSelf.weexInstance componentForRef:elemRef];
        if (!component) {
            return;
        }
        
        [weakSelf performSelectorOnMainThread:@selector(doBlock:) withObject:^() {
            block(component);
        } waitUntilDone:NO];
    });
}

- (void)doBlock:(void (^)())block {
    block();
}

- (MAMapView *)mapView {
    if (_mapView == nil) {
        _mapView = [[MAMapView alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];
        _mapView.showsUserLocation = YES;
        _mapView.userTrackingMode = MAUserTrackingModeFollow;
        _mapView.userTrackingMode = 1;
    }
    return _mapView;
}
@end
