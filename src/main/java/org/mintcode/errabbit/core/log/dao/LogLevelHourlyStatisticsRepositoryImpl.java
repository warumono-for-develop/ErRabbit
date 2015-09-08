package org.mintcode.errabbit.core.log.dao;

import com.mongodb.WriteResult;
import org.mintcode.errabbit.model.Log;
import org.mintcode.errabbit.model.LogLevelDailyStatistics;
import org.mintcode.errabbit.model.LogLevelHourStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by soleaf on 15. 9. 8..
 */
public class LogLevelHourlyStatisticsRepositoryImpl implements LogLevelHourlyStatisticsRepositoryCustom {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void insertStatistic(Log log) {

        try {


            //COLLECTION_PREFIX + ".statistic"
        /*
            log.statstic
                {rabbit : 'rabbitID'
                ,year : year
                ,month : month
                ,day : day
                ,level_error : n
                ,level_info : n
                ...}
         */

            // Extracting collectedDate
            // todo: Consider timezone of client
            Date date = new Date(log.getLoggingEvent().getTimeStamp());
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            Integer year = cal.get(Calendar.YEAR);
            Integer month = cal.get(Calendar.MONTH) + 1; // normal day
            Integer day = cal.get(Calendar.DAY_OF_MONTH);
            Integer hour = cal.get(Calendar.HOUR_OF_DAY);

            DateFormat format = new SimpleDateFormat("yyyyMMdd");
            Integer dateInt = Integer.parseInt(format.format(date));

            // Upsert + $inc
            Query query = new Query();
            query.addCriteria(Criteria.where("rabbitId").is(log.getRabbitId())
                            .andOperator(
                                    Criteria.where("dateInt").is(dateInt),
                                    Criteria.where("year").is(year),
                                    Criteria.where("month").is(month),
                                    Criteria.where("day").is(day),
                                    Criteria.where("hour").is(hour)
                            )
            );

            Update update = new Update().inc("level_" + log.getLoggingEvent().getLevel(), 1);
            mongoOperations.upsert(query, update, LogLevelHourStatistics.class);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    @Override
    public WriteResult deleteDailyStatisticRangeOfLoggingEventDateInt(String rabbitId, Integer begin, Integer end) {
        Query query = new Query();
        query.addCriteria(Criteria.where("rabbitId").is(rabbitId)
                        .andOperator(
                                Criteria.where("dateInt").gte(begin),
                                Criteria.where("dateInt").lte(end)
                        )
        );
        return mongoOperations.remove(query, LogLevelHourStatistics.class);
    }
}