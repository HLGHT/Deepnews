package com.example.po;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@TableName("channel")
@NoArgsConstructor
@AllArgsConstructor
public class News {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField(value="title")
    private String title;
    @TableField(value="url")
    private String url;
    @TableField(value="content")
    private String content;
    @TableField(value="author")
    private String author;
    @TableField(value="intro")
    private String intro;
    @TableField(value = "publish_time")
    private Date publishTime;
    @TableField(value = "media_name")
    private String mediaName;
    @TableField(value = "images")
    private String images;
    @TableField(value = "category")
    private String category;
    @TableField(value = "source")
    private String source;
    @TableField(value = "create_time")
    private Date createTime;
    @TableField(value = "update_time")
    private Date updateTime;
}