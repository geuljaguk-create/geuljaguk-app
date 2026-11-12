package com.geuljaguk.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.geuljaguk.app.widget.BaseWidget;
import com.kakao.sdk.common.KakaoSdk;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KakaoSdk.init(this, getString(R.string.kakao_app_key));   // 카카오 로그인
    }

    @Override
    public void onPause() {
        super.onPause();
        // 앱에서 바꾼 배경·글씨 색을 홈 화면 위젯에도 바로 반영
        try { BaseWidget.refreshAll(this); } catch (Exception ignored) {}
    }
}
