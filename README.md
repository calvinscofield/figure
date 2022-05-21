# 项目简介
这是一个朴素的前后端分离的web项目（前端项目[figure-web](https://github.com/calvinscofield/figure-web)）。根据本人对目前最流行的spring框架、vue框架、http协议的使用和理解，总结出来的一套最佳实践方案，同时加入了本人对精细化权限控制的思考和初步实现。现在以一个整体项目的形式一起分享交流、共同学习进步。  

我在服务器上部署了一份可以直接体验：[49.234.47.129](http://49.234.47.129)

## 用到的技术
spring boot, spring framework, spring security, spring data jpa, vue, element plus, postgresql, etc.

## 权限说明
权限控制精确到每张表的每个个字段的读写权限。例如这样的权限：user:username:r 即表示user表username字段可读。

## 待办事项
计划后续要添加的内容：
* TODO 1：添加通过短信/邮件验证码注册和登录功能（已完成）
* TODO 2：完善前后端数据验证（已完成）
* TODO 3：目前的权限只有对字段级别的读写控制，准备添加对数据库里记录的权限控制（即控制用户可以操作哪些特定的行），举个例子：做到控制某些用户能查看所有人上传的文件，但是只能删除自己上传的文件。

## 附注
![4tx51.jpg](https://s1.328888.xyz/2022/05/08/4tx51.jpg)
![4tP9g.png](https://s1.328888.xyz/2022/05/08/4tP9g.png)
![4taik.jpg](https://s1.328888.xyz/2022/05/08/4taik.jpg)

以收款码的名义微信实名交友。  
这个项目对你有一点帮助？加我微信！有疑问需要进一步的交流？加我微信！  
持续交友，持续更新。  
SHARE CODES, WITH SMILES.

