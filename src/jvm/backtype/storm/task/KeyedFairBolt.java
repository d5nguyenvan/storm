package backtype.storm.task;

import backtype.storm.task.CoordinatedBolt.FinishedCallback;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.KeyedRoundRobinQueue;
import java.util.Map;


public class KeyedFairBolt implements IRichBolt, FinishedCallback {
    private static final long serialVersionUID = 1L;

    IRichBolt _delegate;
    KeyedRoundRobinQueue<Tuple> _rrQueue;
    Thread _executor;
    FinishedCallback _callback;

    public KeyedFairBolt(IRichBolt delegate) {
        _delegate = delegate;
    }

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if(_delegate instanceof FinishedCallback) {
            _callback = (FinishedCallback) _delegate;
        }
        _delegate.prepare(stormConf, context, collector);
        _rrQueue = new KeyedRoundRobinQueue<Tuple>();
        _executor = new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        _delegate.execute(_rrQueue.take());
                    }
                } catch (InterruptedException e) {

                }
            }
        });
        _executor.setDaemon(true);
        _executor.start();
    }

    public void execute(Tuple input) {
        Object key = input.getValue(0);
        _rrQueue.add(key, input);
    }

    public void cleanup() {
        _executor.interrupt();
        _delegate.cleanup();
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        _delegate.declareOutputFields(declarer);
    }

    public void finishedId(Object id) {
        if(_callback!=null) {
            _callback.finishedId(id);
        }
    }
}
