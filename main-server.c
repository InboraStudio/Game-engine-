/****************************************************************************************
 *
 * BSD LICENSE
 *
 * Copyright(c) 2015 Intel Corporation.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * * Neither the name of Intel Corporation nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ***************************************************************************************/
/***
 *      the include file below must be included in algorithm_support.c file.
 ***/
#include "opencore_algo_support.h"

/************
 * the below buffer is of a basis algo event report.
 * Note: the struct type "struct demo_algo_result" must be defined by user
 * in sensor_data_format.h in advance.
 ***************/
static struct demo_algo_result demo_rpt_buf;

/************
 * the algorithm details tha user design.
 ***********/
static int demo_algorithm_process(int16_t accel_data[3], int gyro_data[3])
{
	static int count = 0;

	count++;
	if (count == 100) {
		count = 0;
		return 1;
	}
	return 0;
}

/****************
 * the function below is called when basic algorithm do not want to goto idle for a while.
 * the detail must be designed by user accdding to the algorithm processs.
 ******************/
static int demo_algorithm_hold_idle(void)
{
	return 0;
}

/*********************
 * the callback function below is template of algorirhm's execute function
 * in case of the algorithm demanding accelerometer data.
 **********************/
static int demo_algorithm_exec(void **sensor_data, feed_general_t *feed)
{
	int16_t accel_data[3] = { 0 };
	int gyro_data[3] = { 0 };
	int index_a = GetDemandIdx(feed, SENSOR_ACCELEROMETER);
	int index_g = GetDemandIdx(feed, SENSOR_GYROSCOPE);
	int result = 0;
	int ret = 0;

	if (index_a >= 0 && index_g >= 0) {
		if (sensor_data[index_a] != NULL || sensor_data[index_g] !=
		    NULL) {
			int size = GetSensorDataFrameSize(
				feed->demand[index_a].type,
				feed->demand[index_a].id);
			if (size > 0 && sensor_data[index_a] != NULL)
				memcpy(&accel_data[0], sensor_data[index_a],
				       size);

			size = GetSensorDataFrameSize(
				feed->demand[index_g].type,
				feed->demand[index_g].id);
			if (size > 0 && sensor_data[index_g] != NULL)
				memcpy(&gyro_data[0], sensor_data[index_g],
				       size);

			result = demo_algorithm_process(accel_data, gyro_data);
			if (result == 1) {
				exposed_sensor_t *sensor = GetExposedStruct(
					SENSOR_ALGO_DEMO, DEFAULT_ID);
				if (sensor != NULL) {
					struct demo_algo_result *value =
						(struct demo_algo_result *)
						sensor->
						rpt_data_buf;
					value->type = result;
					value->ax = accel_data[0];
					value->ay = accel_data[1];
					value->az = accel_data[2];
					value->gx = gyro_data[0];
					value->gy = gyro_data[1];
					value->gz = gyro_data[2];
					sensor->ready_flag = 1;
					ret++;
				}
			}
		}
	}

	result = demo_algorithm_hold_idle();
	if (result == 1) {
		STOP_IDLE(feed);
	} else {
		START_IDLE(feed);
	}

	return ret;
}

/***********************
 * the callback function below is called when basic algorithms is started at first.
 **********************/
static int demo_algorithm_init(feed_general_t *feed_ptr)
{
	return 0;
}


/***********************
 * the callback function below is called when basic algorithms is stopped at last.
 **********************/
static int demo_algorithm_deinit(feed_general_t *feed_ptr)
{
	return 0;
}



/*********************
 * the callback function below is called when there is no motion in
 * phyiscal sensor so the all the algorithms have to go to sleep.
 * it is sure when the basic algorithms do not want to go to sleep,
 * can go on processing through HOLD_IDLE(feed) methord.
 ********************/
static int demo_algorithm_goto_idle(feed_general_t *feed_prt)
{
	return 0;
}


/*********************
*  the callbaak function below is called when there is any motion in
*  physical sensor, and all the basic algorithms will be wake by sensor core
*********************/
static int demo_algorithm_out_idle(feed_general_t *feed_prt)
{
	return 0;
}



/************************
 * in the struct sensor_data_data_demand_t,
 * 1. freq : the frequence value(Hz) of physical sensor sampling
 *      that is need by basic algorithm.
 * 2. rt : the reporting time(ms) of sensor_core feeding interval,
 *      which is required for basic algorithm. because of the algorithm delay time.
 ***********************/
sensor_data_demand_t basic_algo_demo_demand[] = {
	{
		.type = SENSOR_ACCELEROMETER,
		.id = DEFAULT_ID,
		.range_max = DEFAULT_VALUE,
		.range_min = DEFAULT_VALUE,
		.freq = 100,
		.rt = 1000,
	},
	{
		.type = SENSOR_GYROSCOPE,
		.id = DEFAULT_ID,
		.range_max = DEFAULT_VALUE,
		.range_min = DEFAULT_VALUE,
		.freq = 100,
		.rt = 1000,
	}
};

/****************************
 * define feed_general_t struct where figure out user's basic
 * algorithm attribute and reference callback.
 * 1. type: the basic_algo type that user can define to
 *      basic_algo_type_t enum in opencore_algo_common.h.
 * 2. demand: describe what physical sensor data to demand in the basic_algo.
 * 3. demand_length: count of demand array.
 * 4. ctl_api: consist of the api what are used to control the basic_algo,
 *      such as init, exex, goto_idle and out_idle
 * 5. <define_feedinit> is used to make out the feed list in opencore.
 *******************************/
static feed_general_t demo_algo = {
	.type = BASIC_ALGO_DEMO,
	.demand = basic_algo_demo_demand,
	.demand_length = sizeof(basic_algo_demo_demand)
			 / sizeof(sensor_data_demand_t),
	.ctl_api = {
		.init = demo_algorithm_init,
		.deinit = demo_algorithm_deinit,
		.exec = demo_algorithm_exec,
		.goto_idle = demo_algorithm_goto_idle,
		.out_idle = demo_algorithm_out_idle,
	},
};
define_feedinit(demo_algo);



/***********************************
 * define exposed_sensor_t struct which is the event reporter of basic_algo.
 * 1.depend_flag: shift value of the type of the basic algo that user defined.
 * 2.type: type of the exposed sensor that  user need define to
 *      ss_sensor_type_t enum in sensor_data_format.h and add this type shift mask
 *      to BOARD_SENSOR_MASK define.
 * 3.rpt_data_buf: the pointer of  report data buf.
 * 4.rpt_data_buf_len: the length of rpt_data_buf.
 **********************************/
static exposed_sensor_t demo_exposed_sensor = {
	.depend_flag = 1 << BASIC_ALGO_DEMO,
	.type = SENSOR_ALGO_DEMO,
	.id = DEFAULT_ID,
	.rpt_data_buf = (void *)&demo_rpt_buf,
	.rpt_data_buf_len = sizeof(struct demo_algo_result),
};
define_exposedinit(demo_exposed_sensor);
