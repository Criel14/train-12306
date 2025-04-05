package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.criel.train.business.domain.generated.DailyTrainTicket;
import com.criel.train.business.domain.generated.TrainStation;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.DailyTrainStation;
import com.criel.train.business.domain.generated.DailyTrainStationExample;
import com.criel.train.business.mapper.DailyTrainStationMapper;
import com.criel.train.business.req.DailyTrainStationQueryReq;
import com.criel.train.business.req.DailyTrainStationSaveReq;
import com.criel.train.business.resp.DailyTrainStationQueryResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainStationService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainStationService.class);

    @Autowired
    private DailyTrainStationMapper dailyTrainStationMapper;

    @Autowired
    private TrainStationService trainStationService;

    public void save(DailyTrainStationSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(req, DailyTrainStation.class);

        // 自动计算停站时长
        long diffMillis = req.getOutTime().getTime() - req.getInTime().getTime();
        if (diffMillis < 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_IN_TIME_OUT_TIME_ERROR);
        }
        Date stopTime = new Date(diffMillis);
        dailyTrainStation.setStopTime(stopTime);

        if (ObjectUtil.isNull(dailyTrainStation.getId())) {
            dailyTrainStation.setId(SnowflakeUtil.getSnowflakeNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.insert(dailyTrainStation);
        } else {
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.updateByPrimaryKey(dailyTrainStation);
        }
    }

    public PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.setOrderByClause("date desc, train_code asc, `index` asc");
        DailyTrainStationExample.Criteria criteria = dailyTrainStationExample.createCriteria();

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
        List<DailyTrainStation> dailyTrainStationList = dailyTrainStationMapper.selectByExample(dailyTrainStationExample);

        PageInfo<DailyTrainStation> pageInfo = new PageInfo<>(dailyTrainStationList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainStationQueryResp> list = BeanUtil.copyToList(dailyTrainStationList, DailyTrainStationQueryResp.class);

        PageResp<DailyTrainStationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainStationMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成对应日期、对应车次的车站数据
     *
     * @param date
     * @param trainCode
     */
    @Transactional
    public void genDaily(Date date, String trainCode) {
        // 删除原有数据
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        dailyTrainStationMapper.deleteByExample(dailyTrainStationExample);

        // 生成
        LOG.info("开始生成{}的{}车次的车站信息", DateUtil.formatDate(date), trainCode);

        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);
        if (trainStationList == null || trainStationList.isEmpty()) {
            LOG.info("{}车次没有车站信息，无法生成每日车站数据", trainCode);
            return;
        }
        for (TrainStation trainStation : trainStationList) {
            genDailyTrainStation(date, trainStation);
        }

        LOG.info("结束生成{}的{}车次的车站信息", DateUtil.formatDate(date), trainCode);
    }

    /**
     * 生成单日单个车次车站信息
     *
     * @param date
     * @param trainStation
     */
    private void genDailyTrainStation(Date date, TrainStation trainStation) {
        Date now = new Date();
        DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(trainStation, DailyTrainStation.class);
        dailyTrainStation.setId(SnowflakeUtil.getSnowflakeNextId());
        dailyTrainStation.setCreateTime(now);
        dailyTrainStation.setUpdateTime(now);
        dailyTrainStation.setDate(date);
        dailyTrainStationMapper.insert(dailyTrainStation);
    }

    /**
     * 根据日期和车次返回车站数量
     * @param date
     * @param trainCode
     * @return
     */
    public long countByTrainCode(Date date, String trainCode) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        return dailyTrainStationMapper.countByExample(dailyTrainStationExample);
    }
}
