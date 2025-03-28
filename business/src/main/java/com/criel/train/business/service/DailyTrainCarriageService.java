package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.criel.train.business.domain.generated.*;
import com.criel.train.business.enumeration.SeatColEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.mapper.DailyTrainCarriageMapper;
import com.criel.train.business.req.DailyTrainCarriageQueryReq;
import com.criel.train.business.req.DailyTrainCarriageSaveReq;
import com.criel.train.business.resp.DailyTrainCarriageQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainCarriageService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainCarriageService.class);

    @Autowired
    private DailyTrainCarriageMapper dailyTrainCarriageMapper;

    @Autowired
    private TrainCarriageService trainCarriageService;

    public void save(DailyTrainCarriageSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(req, DailyTrainCarriage.class);

        // 由座位类型得到列数，进而得总座位数
        List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(dailyTrainCarriage.getSeatType());
        dailyTrainCarriage.setColCount(seatColEnums.size());
        dailyTrainCarriage.setSeatCount(dailyTrainCarriage.getRowCount() * seatColEnums.size());

        if (ObjectUtil.isNull(dailyTrainCarriage.getId())) {
            dailyTrainCarriage.setId(SnowflakeUtil.getSnowflakeNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        } else {
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.updateByPrimaryKey(dailyTrainCarriage);
        }
    }

    public PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req) {
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();
        dailyTrainCarriageExample.setOrderByClause("date desc, train_code asc, `index` asc");
        DailyTrainCarriageExample.Criteria criteria = dailyTrainCarriageExample.createCriteria();

        // 根据date查询
        if (ObjectUtil.isNotNull(req.getDate())) {
            criteria.andDateEqualTo(req.getDate());
        }
        // 根据trainCode查询
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainCarriage> dailyTrainCarriageList = dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);

        PageInfo<DailyTrainCarriage> pageInfo = new PageInfo<>(dailyTrainCarriageList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainCarriageQueryResp> list = BeanUtil.copyToList(dailyTrainCarriageList, DailyTrainCarriageQueryResp.class);

        PageResp<DailyTrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainCarriageMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成对应日期、对应车次的车站数据
     *
     * @param date
     * @param trainCode
     */
    public void genDaily(Date date, String trainCode) {
        // 删除原有数据
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        dailyTrainCarriageMapper.deleteByExample(dailyTrainCarriageExample);

        // 生成
        List<TrainCarriage> trainCarriageList = trainCarriageService.selectByTrainCode(trainCode);
        if (trainCarriageList == null || trainCarriageList.isEmpty()) {
            LOG.info("{}车次没有车站信息，无法生成每日车厢数据", trainCode);
            return;
        }
        for (TrainCarriage trainCarriage : trainCarriageList) {
            genDailyTrainCarriage(date, trainCarriage);
        }
    }

    /**
     * 生成单日单个车次车厢信息
     *
     * @param date
     * @param trainCarriage
     */
    private void genDailyTrainCarriage(Date date, TrainCarriage trainCarriage) {
        LOG.info("开始生成{}的{}车次的车厢信息", DateUtil.formatDate(date), trainCarriage.getTrainCode());

        Date now = new Date();
        DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(trainCarriage, DailyTrainCarriage.class);
        dailyTrainCarriage.setId(SnowflakeUtil.getSnowflakeNextId());
        dailyTrainCarriage.setCreateTime(now);
        dailyTrainCarriage.setUpdateTime(now);
        dailyTrainCarriage.setDate(date);
        dailyTrainCarriageMapper.insert(dailyTrainCarriage);

        LOG.info("结束生成{}的{}车次的车厢信息", DateUtil.formatDate(date), trainCarriage.getTrainCode());
    }
}
