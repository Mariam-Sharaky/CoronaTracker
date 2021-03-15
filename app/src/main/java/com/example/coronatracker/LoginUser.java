package com.example.coronatracker;

public class LoginUser {
    private String email;
    private String password;
    private String fname;
    private String lname;
    private String phone;


    public LoginUser(String email, String password,String fname, String lname, String phone){
        this.email = email;
        this.password = password;
        this.fname = fname;
        this.lname = lname;
        this.phone = phone;
    }

}
