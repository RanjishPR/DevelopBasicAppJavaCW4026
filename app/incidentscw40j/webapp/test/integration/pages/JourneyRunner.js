sap.ui.define([
    "sap/fe/test/JourneyRunner",
	"ns/incidentscw40j/test/integration/pages/IncidentsList.gen",
	"ns/incidentscw40j/test/integration/pages/IncidentsObjectPage.gen"
], function (JourneyRunner, IncidentsListGenerated, IncidentsObjectPageGenerated) {
    'use strict';

    const runner = new JourneyRunner({
        launchUrl: sap.ui.require.toUrl('ns/incidentscw40j') + '/test/flpSandbox.html#nsincidentscw40j-tile',
        pages: {
			onTheIncidentsListGenerated: IncidentsListGenerated,
			onTheIncidentsObjectPageGenerated: IncidentsObjectPageGenerated
        },
        async: true
    });

    return runner;
});

