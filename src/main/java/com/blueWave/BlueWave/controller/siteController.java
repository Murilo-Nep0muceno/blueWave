package com.blueWave.BlueWave.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping
public class siteController {


    @GetMapping("/sucesso")
    public String sucesso(){
        return "sucesso";
    }

    @GetMapping("/")
    public String home(){

        return "/index";
    }

    @GetMapping("/quemSomoss")
    public ModelAndView quemSomos(){
        ModelAndView mv = new ModelAndView("quemSomoss");

        return mv;
    }



}
