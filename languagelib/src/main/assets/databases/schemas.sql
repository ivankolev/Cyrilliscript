DROP TABLE IF EXISTS mean_confidence_events;
CREATE TABLE mean_confidence_events
(
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  mean_confidence ,
  sqltime TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS mean_confidence_events_totals;
CREATE TABLE mean_confidence_events_totals
(
  m_c_events_rows_count INT,
  m_c_events_rows_avg INT
);

CREATE TRIGGER update_totals
  AFTER
  INSERT
  ON
    mean_confidence_events
  FOR EACH ROW
BEGIN UPDATE
  mean_confidence_events_totals
SET m_c_events_rows_avg = (NEW.mean_confidence + m_c_events_rows_count * m_c_events_rows_avg) /
                          (m_c_events_rows_count + 1),
  m_c_events_rows_count = m_c_events_rows_count + 1;
END;

INSERT INTO mean_confidence_events_totals (m_c_events_rows_count, m_c_events_rows_avg) VALUES (0, 0) ;
