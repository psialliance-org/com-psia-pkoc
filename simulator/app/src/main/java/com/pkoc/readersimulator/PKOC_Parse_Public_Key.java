package com.pkoc.readersimulator;

import android.app.Activity;

public class PKOC_Parse_Public_Key extends Activity {
/*
    private TextView textView;

    public void parsePKOCPublicKey(byte[] response) {

        runOnUiThread(() -> {
        int offset = 0;
        while (offset < response.length) {
            byte tag1 = response[offset++];
            int length1 = response[offset++];
            byte[] value = Arrays.copyOfRange(response, offset, offset + length1);
            offset += length1;
            Log.d(TAG,"tag is: " + tag1);
            switch (tag1) {
                case (byte) 0x5A:
                    // Public Key
                    String publicKey1 = bytesToHex(value);
                    Log.d("NFC", "Public Key: \n" + publicKey1);

                    // Parse the public key
                    if (publicKey1.length() == 130) {
                        String header = publicKey1.substring(0, 2);
                        String xPortion = publicKey1.substring(2, 66);
                        String yPortion = publicKey1.substring(66, 130);

                        // Extract 64 Bit and 128 Bit Credentials from X Portion
                        String credential64Bit = xPortion.substring(xPortion.length() - 16);
                        String credential128Bit = xPortion.substring(xPortion.length() - 32);

                        // Convert Hex to Decimal
                        String credential64BitDecimal = new BigInteger(credential64Bit, 16).toString(10);
                        String credential128BitDecimal = new BigInteger(credential128Bit, 16).toString(10);
                        String credential256BitDecimal = new BigInteger(xPortion, 16).toString(10);

                        // Use SpannableStringBuilder to build the final text
                        SpannableStringBuilder formattedText = new SpannableStringBuilder();

                        // Apply bold style to the "Public Key:" text with black color and size 14
                        SpannableString publicKeyHeader = new SpannableString("Public Key: \n");
                        publicKeyHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeyHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeyHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, publicKeyHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(publicKeyHeader);

                        // Apply colors and font size to the Public Key
                        //Header
                        SpannableStringBuilder publicKeySpannable = new SpannableStringBuilder(publicKey1);
                        publicKeySpannable.setSpan(new BackgroundColorSpan(Color.WHITE), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeySpannable.setSpan(new AbsoluteSizeSpan(14, true), 0, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        //256 bit - xPortion
                        publicKeySpannable.setSpan(new StyleSpan(Typeface.BOLD), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeySpannable.setSpan(new BackgroundColorSpan(Color.parseColor("#A41D23")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#2FB56A")), 2, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        //128 bit
                        publicKeySpannable.setSpan(new StyleSpan(Typeface.ITALIC), 35, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#C0C0C0")), 34, 50, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Light blue

                        //64 bit
                        publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#34ACBB")), 51, 66, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        //y portion
                        publicKeySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#707173")), 66, 130, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        formattedText.append(publicKeySpannable);

                        // Append the rest of the text with specified colors and font size for Headers and values
                        // This is ***AFTER THE PUBIC KEY DISPLAY***

                        SpannableString headerHeader = new SpannableString("\n\nHeader: (Not Used)\n");
                        headerHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        headerHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        headerHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, headerHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(headerHeader);

                        formattedText.append(applyColorAndSize(header, 0, header.length(), Color.WHITE, (Color.parseColor("#707173")), 14,false));

                        // x-Portion of the public key
                        SpannableString portionxHeader = new SpannableString("\n\nX Portion 256 Bit HEX: \n");
                        portionxHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, portionxHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        portionxHeader.setSpan(new ForegroundColorSpan(Color.BLACK), 0, portionxHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        portionxHeader.setSpan(new AbsoluteSizeSpan(14, true), 0, portionxHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(portionxHeader);

                        formattedText.append(applyColorAndSize(xPortion, 0, xPortion.length(), (Color.parseColor("#A41D23")), (Color.parseColor("#2FB56A")), 14, true));

                        // 256 bit decimal of the public key
                        SpannableString decimalTFSb = new SpannableString("\n\n256 Bit Decimal: \n");
                        decimalTFSb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalTFSb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalTFSb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalTFSb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(decimalTFSb);

                        formattedText.append(applyColorAndSize(credential256BitDecimal, 0, credential256BitDecimal.length(), Color.parseColor("#A41D23"), Color.parseColor("#2FB56A"), 14, true));

                        // 128 bit hex of the public key
                        SpannableString hexOTEb = new SpannableString("\n\n128 Bit HEX: \n");
                        hexOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        hexOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        hexOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(hexOTEb);

                        formattedText.append(applyColorAndSize(credential128Bit, 0, credential128Bit.length(), Color.parseColor("#A41D23"), Color.parseColor("#C0C0C0"), 14, true));

                        // 128 bit decimal of the public key
                        SpannableString decimalOTEb = new SpannableString("\n\n128 Bit Decimal: \n");
                        decimalOTEb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalOTEb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalOTEb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalOTEb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(decimalOTEb);

                        formattedText.append(applyColorAndSize(credential128BitDecimal, 0, credential128BitDecimal.length(), Color.parseColor("#A41D23"), Color.parseColor("#C0C0C0"),14, true)); // Light blue


                        // 64 bit hex of the public key
                        SpannableString hexSFb = new SpannableString("\n\n64 Bit Hex: \n");
                        hexSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        hexSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        hexSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, hexSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(hexSFb);

                        formattedText.append(applyColorAndSize(credential64Bit, 0, credential64Bit.length(), Color.parseColor("#A41D23"), Color.parseColor("#34ACBB"), 14, true));

                        // 64 bit decimal of the public key
                        SpannableString decimalSFb = new SpannableString("\n\n64 Bit Decimal: \n");
                        decimalSFb.setSpan(new StyleSpan(Typeface.BOLD), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalSFb.setSpan(new ForegroundColorSpan(Color.BLACK), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        decimalSFb.setSpan(new AbsoluteSizeSpan(14, true), 0, decimalSFb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(decimalSFb);

                        formattedText.append(applyColorAndSize(credential64BitDecimal, 0, credential64BitDecimal.length(), Color.parseColor("#A41D23"), Color.parseColor("#34ACBB"), 14, true));

                        // Y-Portion of the public key
                        SpannableString portionYKey = new SpannableString("\n\nY Portion HEX (Not Used): \n");
                        portionYKey.setSpan(new StyleSpan(Typeface.BOLD), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        portionYKey.setSpan(new ForegroundColorSpan(Color.BLACK), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        portionYKey.setSpan(new AbsoluteSizeSpan(14, true), 0, portionYKey.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        formattedText.append(portionYKey);

                        formattedText.append(applyColorAndSize(yPortion, 0, yPortion.length(), Color.WHITE, Color.parseColor("#707173"), 14, false));

                        // Set the formatted text to the TextView
                        textView.setText(formattedText);

                        // Hide reader detail button
                        Button rdrButton = findViewById(R.id.rdrButton);
                        rdrButton.setVisibility(View.GONE);

                        // Set up the email button
                        Button emailButton = findViewById(R.id.emailButton);
                        emailButton.setVisibility(View.VISIBLE); // Make the button visible
                        emailButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendEmail();
                            }
                        });

                        // Set up the scan button
                        Button scanButton = findViewById(R.id.scanButton);
                        scanButton.setVisibility(View.VISIBLE); // Make the button visible
                        scanButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetToScanScreen();
                            }
                        });
                    } else {
                        // Set the formatted public key if parsing is not applicable
                        SpannableString formattedText = formatText("Public Key: " + publicKey, 14, Color.BLACK);
                        textView.setText(formattedText);
                    }

                    // Hide other fields
                    readerLocationUUIDView.setVisibility(View.GONE);
                    readerSiteUUIDView.setVisibility(View.GONE);
                    sitePublicKeyView.setVisibility(View.GONE);
                    nfcadvertisingStatusView.setVisibility(View.GONE);
                    bleadvertisingStatusView.setVisibility(View.GONE);
                    break;

                case (byte) 0x9E:
                    // Digital Signature
                    String signature = bytesToHex(value);
                    Log.d("NFC", "Digital Signature: " + signature);
                    break;

                case (byte) 0x4C:
                    // Reader Location UUID
                    String readerLocationUUID = bytesToHex(value);
                    Log.d("NFC", "Reader Location UUID: " + readerLocationUUID);
                    readerLocationUUIDView.setText("Reader Location UUID: " + readerLocationUUID);
                    break;

                case (byte) 0x4D:
                    // Reader Site UUID
                    String readerSiteUUID = bytesToHex(value);
                    Log.d("NFC", "Reader Site UUID: " + readerSiteUUID);
                    readerSiteUUIDView.setText("Reader Site UUID: " + readerSiteUUID);
                    break;

                case (byte) 0x4E:
                    // Site Public Key
                    String sitePublicKey = bytesToHex(value);
                    Log.d("NFC", "Site Public Key: " + sitePublicKey);
                    sitePublicKeyView.setText("Site Public Key: " + sitePublicKey);
                    break;

                case (byte) 0x4F:
                    // Advertising Status
                    String advertisingStatus = bytesToHex(value);
                    Log.d("NFC", "<b>Advertising Status:</b> " + advertisingStatus);
                    nfcadvertisingStatusView.setText("<b>Advertising Status:</b> " + advertisingStatus);
                    break;

                default:
                    Log.d("NFC/BLE", "Unknown Request: " + tag1);
                    break;
            }
        }
    });
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Define the formatText method
    private SpannableString formatText(String text, int fontSize, int color) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new AbsoluteSizeSpan(fontSize, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    // Define the formatText method with HEX color value option
    private SpannableString formatTextH(String text, int textSize, String hexColor) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new AbsoluteSizeSpan(textSize, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor(hexColor)), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    // Helper method to apply background, text color, font size, and bold attribute to a specific range of text
    private SpannableStringBuilder applyColorAndSize(String text, int start, int end, int bgColor, int textColor, int fontSize, boolean isBold) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        spannable.setSpan(new BackgroundColorSpan(bgColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(fontSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (isBold) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

*/

}
