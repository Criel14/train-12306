package com.criel.train.business.mapper.customer;

import java.util.Date;

public interface SkTokenMapperCustomer {

    int decreaseCount(Date date, String trainCode, int count);
}