package com.geuljaguk.app.widget;

import com.geuljaguk.app.R;

public class QuoteSmallWidget extends BaseWidget {
    @Override protected int layout() { return R.layout.widget_quote_small; }
    @Override protected boolean quoteMode() { return true; }
}
