package com.phaseshiftlab.ocrlib;

public class LanguageItem {
    private String languageName;
    private String languageFullName;
    private String languageDescription;
    private Boolean isInstalled;

    public LanguageItem(String languageName, String languageFullName, String languageDescription, Boolean isInstalled) {
        this.languageName = languageName;
        this.languageFullName = languageFullName;
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

    public String getLanguageFullName() {
        return languageFullName;
    }

    public void setLanguageFullName(String languageFullName) {
        this.languageFullName = languageFullName;
    }
}
