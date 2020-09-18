package com.nhom08.doanlaptrinhandroid.DTO;

public class Wp_user {
    private int ID;
    private String user_login;
    private String user_pass;
    private String user_nicename;
    private String user_email;
    private String user_url;
    private String user_registered;
    private String user_activation_key;
    private int user_status;
    private String display_name;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getUser_login() {
        return user_login;
    }

    public void setUser_login(String user_login) {
        this.user_login = user_login;
    }

    public String getUser_pass() {
        return user_pass;
    }

    public void setUser_pass(String user_pass) {
        this.user_pass = user_pass;
    }

    public String getUser_nicename() {
        return user_nicename;
    }

    public void setUser_nicename(String user_nicename) {
        this.user_nicename = user_nicename;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_url() {
        return user_url;
    }

    public void setUser_url(String user_url) {
        this.user_url = user_url;
    }

    public String getUser_registered() {
        return user_registered;
    }

    public void setUser_registered(String user_registered) {
        this.user_registered = user_registered;
    }

    public String getUser_activation_key() {
        return user_activation_key;
    }

    public void setUser_activation_key(String user_activation_key) {
        this.user_activation_key = user_activation_key;
    }

    public int getUser_status() {
        return user_status;
    }

    public void setUser_status(int user_status) {
        this.user_status = user_status;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    @Override
    public String toString() {
        return "Wp_user{" +
                "ID=" + ID +
                ", user_login='" + user_login + '\'' +
                ", user_pass='" + user_pass + '\'' +
                ", user_nicename='" + user_nicename + '\'' +
                ", user_email='" + user_email + '\'' +
                ", user_url='" + user_url + '\'' +
                ", user_registered='" + user_registered + '\'' +
                ", user_activation_key='" + user_activation_key + '\'' +
                ", user_status=" + user_status +
                ", display_name='" + display_name + '\'' +
                '}';
    }
}
