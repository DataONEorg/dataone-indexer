package org.dataone.cn.indexer.processor;

import static org.junit.Assert.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

public class TestIndexTaskProcessorConcurrency {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testTrue()
    {
        ;
    }

       @Test
    public void runThroughSchedulerBehavior() throws IOException, SchedulerException {


            Properties properties = new Properties();
            properties.load(this.getClass().getResourceAsStream(
                    "/org/dataone/configuration/quartz.properties"));
            SchedulerFactory schedulerFactory = new StdSchedulerFactory(properties);
            Scheduler scheduler = schedulerFactory.getScheduler();

            JobDetail job = newJob(MockIndexTaskProcessorJob.class)
                    .withIdentity("test_job", "test_group")
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("test_trigger", "test_group")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
                    .build();
            
            scheduler.scheduleJob(job, trigger);
            
            
            System.out.println("============= scheduler START ==============" + new Date());
            scheduler.start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new SchedulerException("Interrupted", e);
            }
//            System.out.println("============= scheduler STANDBY ==============" + new Date());
//            scheduler.standby();
//            
//            
//
//            
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new SchedulerException("Scheduler Interrupted", e);
//            }
            
            System.out.println("============= attempt to kill the Job ==============" + new Date());
            try {
                if (scheduler.isStarted()) {
                    scheduler.standby();
//                    scheduler.unscheduleJob(new TriggerKey(QUARTZ_PROCESSOR_TRIGGER, QUARTZ_PROCESSOR_GROUP));
                    MockIndexTaskProcessorJob.interruptCurrent();
                    
                    while (!(scheduler.getCurrentlyExecutingJobs().isEmpty())) {
                        System.out.println(String.format("%d jobs executing,  waiting for them to complete...", 
                                scheduler.getCurrentlyExecutingJobs().size()));
                        
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            System.out.println("Sleep interrupted. check again!");
                        }
                    }
                    System.out.println("Job scheduler finish executing all jobs.");
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            System.out.println("============= continue to wait 5 sec... ==============" + new Date());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new SchedulerException("Interrupted", e);
            }
            
            
            
//            System.out.println("============= scheduler START (again) ==============" + new Date());
//            scheduler. start();
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new SchedulerException("Interrupted", e);
//            }
            System.out.println("============= scheduler SHUTDOWN ==============" + new Date());
            scheduler.shutdown(false);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                throw new SchedulerException("Interrupted", e);
            }
            System.out.println("============= DONE !!!!!!!!!! ==============" + new Date());
    }

}
