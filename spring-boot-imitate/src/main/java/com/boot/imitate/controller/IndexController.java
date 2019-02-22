package com.boot.imitate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author lilinshen
 * @title 请填写标题
 * @description 请填写相关描述
 * @date 2019/2/22 15:10
 */
@Controller
public class IndexController {
	@RequestMapping("/index")
	@ResponseBody
	public String index() {
		return "index";
	}
}
