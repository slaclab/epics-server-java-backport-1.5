package org.epics;

import org.epics.pvaccess.PVAException;
import org.epics.pvaccess.server.rpc.RPCRequestException;
import org.epics.pvaccess.server.rpc.RPCServer;
import org.epics.pvaccess.server.rpc.RPCService;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.pv.Status.StatusType;

public class PVAServer {

    private final static FieldCreate fieldCreate = FieldFactory.getFieldCreate();

    private final static Structure resultStructure =
            fieldCreate.createFieldBuilder().
                    add("c", ScalarType.pvDouble).
                    createStructure();

    static class SumServiceImpl implements RPCService {
        public PVStructure request(PVStructure args) throws RPCRequestException {

            // NTURI support
            if (args.getStructure().getID().startsWith("epics:nt/NTURI:1."))
                args = args.getStructureField("query");

            // get fields and check their existence
            PVString af = args.getStringField("a");
            PVString bf = args.getStringField("b");
            if (af == null || bf == null)
                throw new RPCRequestException(StatusType.ERROR, "scalar 'a' and 'b' fields are required");

            // convert
            double a, b;
            try {
                a = Double.parseDouble(af.get());
                b = Double.parseDouble(bf.get());
            } catch (Throwable th) {
                throw new RPCRequestException(StatusType.ERROR, "failed to convert arguments to double", th);

            }

            PVStructure result = PVDataFactory.getPVDataCreate().createPVStructure(resultStructure);
            result.getDoubleField("c").put(a + b);

            return result;
        }
    }

    public static void main(String[] args) throws PVAException {

        if (args.length < 1) {
            throw new RuntimeException("PV not specified: usage\n" +
                    " PVAServer service \n" +
                    "e.g. \n" +
                    " PVAServer AIDA:SAMPLE:DEVICE1");
        }
        // Create a client to the service specified
        String pv = args[0];

        RPCServer server = new RPCServer();

        server.registerService(pv, new SumServiceImpl());
        // you can register as many services as you want here ...

        server.printInfo();
        server.run(0);
    }

}
