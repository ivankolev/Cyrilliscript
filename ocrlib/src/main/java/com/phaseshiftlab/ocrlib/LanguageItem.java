package com.phaseshiftlab.ocrlib;

public class LanguageItem {
    private String languageName;
    private String languageDescription;
    private Boolean isInstalled;

    public LanguageItem(String languageName, String languageDescription, Boolean isInstalled) {
        this.languageName = languageName;
        this.languageDescription = languageDescription;
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

    public String getLanguageDescription() {
        return languageDescription;
    }

    public void setLanguageDescription(String languageDescription) {
        this.languageDescription = languageDescription;
    }
}
