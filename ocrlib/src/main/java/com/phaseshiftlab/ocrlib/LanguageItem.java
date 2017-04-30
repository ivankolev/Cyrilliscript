package com.phaseshiftlab.ocrlib;

public class LanguageItem {
    private String languageName;
    private Boolean isInstalled;

    public LanguageItem(String languageName, Boolean isInstalled) {
        this.languageName = languageName;
        this.isInstalled = isInstalled;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public Boolean getInstalled() {
        return isInstalled;
    }

    public void setInstalled(Boolean installed) {
        isInstalled = installed;
    }
}
