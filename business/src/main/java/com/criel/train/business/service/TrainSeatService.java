package com.criel.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.criel.train.business.domain.generated.TrainCarriage;
import com.criel.train.business.enumeration.SeatColEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.criel.train.common.resp.PageResp;
import com.criel.train.common.util.SnowflakeUtil;
import com.criel.train.business.domain.generated.TrainSeat;
import com.criel.train.business.domain.generated.TrainSeatExample;
import com.criel.train.business.mapper.TrainSeatMapper;
import com.criel.train.business.req.TrainSeatQueryReq;
import com.criel.train.business.req.TrainSeatSaveReq;
import com.criel.train.business.resp.TrainSeatQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainSeatService.class);

    @Autowired
    private TrainSeatMapper trainSeatMapper;

    @Autowired
    private TrainCarriageService trainCarriageService;

    public void save(TrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        TrainSeat trainSeat = BeanUtil.copyProperties(req, TrainSeat.class);
        if (ObjectUtil.isNull(trainSeat.getId())) {
            trainSeat.setId(SnowflakeUtil.getSnowflakeNextId());
            trainSeat.setCreateTime(now);
            trainSeat.setUpdateTime(now);
            trainSeatMapper.insert(trainSeat);
        } else {
            trainSeat.setUpdateTime(now);
            trainSeatMapper.updateByPrimaryKey(trainSeat);
        }
    }

    public PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req) {
        TrainSeatExample trainSeatExample = new TrainSeatExample();
        trainSeatExample.setOrderByClause("train_code asc, carriage_index asc, carriage_seat_index asc");
        TrainSeatExample.Criteria criteria = trainSeatExample.createCriteria();

        // 根据车次编号查询
        if (ObjectUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainSeat> trainSeatList = trainSeatMapper.selectByExample(trainSeatExample);

        PageInfo<TrainSeat> pageInfo = new PageInfo<>(trainSeatList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<TrainSeatQueryResp> list = BeanUtil.copyToList(trainSeatList, TrainSeatQueryResp.class);

        PageResp<TrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        trainSeatMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据车次编号生成车厢中的座位信息
     *
     * @param trainCode
     */
    @Transactional
    public void genTrainSeat(String trainCode) {
        DateTime now = DateTime.now();

        // 清空原本的座位信息
        TrainSeatExample trainSeatExample = new TrainSeatExample();
        TrainSeatExample.Criteria criteria = trainSeatExample.createCriteria();
        criteria.andTrainCodeEqualTo(trainCode);
        trainSeatMapper.deleteByExample(trainSeatExample);

        // 查找遍历trainCode的车厢
        List<TrainCarriage> trainCarriageList = trainCarriageService.selectByTrainCode(trainCode);
        trainCarriageList.forEach(trainCarriage -> {
            int rowCount = trainCarriage.getRowCount();
            String seatType = trainCarriage.getSeatType();
            // 根据座位类型筛选对应的列
            List<SeatColEnum> colList = SeatColEnum.getColsByType(seatType);
            // 车厢内的座序
            int carriageSeatIndex = 1;

            // 遍历生成座位
            for (int row = 1; row <= rowCount; row++) {
                for (SeatColEnum col : colList) {
                    TrainSeat trainSeat = new TrainSeat(
                            SnowflakeUtil.getSnowflakeNextId(),
                            trainCode,
                            trainCarriage.getIndex(),
                            StrUtil.fillBefore(String.valueOf(row), '0', 2),
                            col.getCode(),
                            seatType,
                            carriageSeatIndex++,
                            now, now
                    );
                    trainSeatMapper.insertSelective(trainSeat);
                }
            }
        });
    }
}
