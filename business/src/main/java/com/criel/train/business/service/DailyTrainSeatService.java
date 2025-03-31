package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.criel.train.business.domain.generated.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.mapper.DailyTrainSeatMapper;
import com.criel.train.business.req.DailyTrainSeatQueryReq;
import com.criel.train.business.req.DailyTrainSeatSaveReq;
import com.criel.train.business.resp.DailyTrainSeatQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Autowired
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Autowired
    private TrainSeatService trainSeatService;

    @Autowired
    private TrainStationService trainStationService;

    public void save(DailyTrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(req, DailyTrainSeat.class);
        if (ObjectUtil.isNull(dailyTrainSeat.getId())) {
            dailyTrainSeat.setId(SnowflakeUtil.getSnowflakeNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        } else {
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.updateByPrimaryKey(dailyTrainSeat);
        }
    }

    public PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("date desc, train_code asc, carriage_index asc, carriage_seat_index asc");
        DailyTrainSeatExample.Criteria criteria = dailyTrainSeatExample.createCriteria();

        // 根据trainCode查询
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainSeat> dailyTrainSeatList = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);

        PageInfo<DailyTrainSeat> pageInfo = new PageInfo<>(dailyTrainSeatList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainSeatQueryResp> list = BeanUtil.copyToList(dailyTrainSeatList, DailyTrainSeatQueryResp.class);

        PageResp<DailyTrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainSeatMapper.deleteByPrimaryKey(id);
    }


    /**
     * 查找相应日期、车次下，相应座位类型的数量
     * 该类型不存在则返回-1
     *
     * @param date
     * @param trainCode
     * @param seatTypeCode
     * @return
     */
    public int countSeat(Date date, String trainCode, String seatTypeCode) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode).andSeatTypeEqualTo(seatTypeCode);
        long seatCount = dailyTrainSeatMapper.countByExample(dailyTrainSeatExample);
        if (seatCount == 0L) {
            return -1;
        }
        return Math.toIntExact(seatCount);
    }


    /**
     * 生成对应日期、对应车次的座位数据
     *
     * @param date
     * @param trainCode
     */
    @Transactional
    public void genDaily(Date date, String trainCode) {
        // 删除原有数据
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        dailyTrainSeatMapper.deleteByExample(dailyTrainSeatExample);

        // 生成
        LOG.info("开始生成{}的{}车次的车厢信息", DateUtil.formatDate(date), trainCode);

        List<TrainSeat> trainSeatList = trainSeatService.selectByTrainCode(trainCode);
        if (trainSeatList == null || trainSeatList.isEmpty()) {
            LOG.info("{}车次没有座位信息，无法生成每日座位数据", trainCode);
            return;
        }

        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);
        String defaultSell = StrUtil.fillBefore("", '0', trainStationList.size() - 1);

        for (TrainSeat trainSeat : trainSeatList) {
            genDailyTrainSeat(date, trainSeat, defaultSell);
        }

        LOG.info("结束生成{}的{}车次的车厢信息", DateUtil.formatDate(date), trainCode);
    }

    /**
     * 生成单日单个车次座位信息
     *
     * @param date
     * @param trainSeat
     */
    private void genDailyTrainSeat(Date date, TrainSeat trainSeat, String defaultSell) {
        Date now = new Date();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(trainSeat, DailyTrainSeat.class);
        dailyTrainSeat.setId(SnowflakeUtil.getSnowflakeNextId());
        dailyTrainSeat.setCreateTime(now);
        dailyTrainSeat.setUpdateTime(now);
        dailyTrainSeat.setDate(date);
        dailyTrainSeat.setSell(defaultSell);
        dailyTrainSeatMapper.insert(dailyTrainSeat);
    }

    /**
     * 根据车次、车厢、日期，查询该车厢所有座位，升序
     * @param carriageIndex
     * @param date
     * @param trainCode
     * @return
     */
    public List<DailyTrainSeat> selectByCarriageIndexAndDateAndTrainCode(int carriageIndex, Date date, String trainCode) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("carriage_seat_index asc");
        dailyTrainSeatExample.createCriteria().andCarriageIndexEqualTo(carriageIndex).andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        return dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
    }

    /**
     * 更新DailyTrainSeat的sell字段
     * @param id
     * @param sell
     */
    public void updateSellById(Long id, String sell) {
        DailyTrainSeat dailyTrainSeat = new DailyTrainSeat();
        dailyTrainSeat.setId(id);
        dailyTrainSeat.setSell(sell);
        dailyTrainSeat.setUpdateTime(new Date());
        dailyTrainSeatMapper.updateByPrimaryKeySelective(dailyTrainSeat);
    }
}
