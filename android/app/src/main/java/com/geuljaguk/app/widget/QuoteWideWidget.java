package com.geuljaguk.app.widget;

import com.geuljaguk.app.R;

public class QuoteWideWidget extends BaseWidget {
    @Override protected int layout() { return R.layout.widget_quote_wide; }
    @Override protected boolean quoteMode() { return true; }
}
