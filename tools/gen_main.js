'use strict';
const core=require('./gen_core');
const b1=require('./gen_b1');
const {buildInfra}=require('./gen_b2');
b1.buildResidential();b1.buildOffice();b1.convenienceSet();
b1.gasStation();b1.mall();b1.supermarket();b1.parkingMulti();b1.restaurant();
buildInfra();
core.finish();
