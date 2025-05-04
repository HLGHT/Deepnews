package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.po.News;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsMapper extends BaseMapper<News> {
}
