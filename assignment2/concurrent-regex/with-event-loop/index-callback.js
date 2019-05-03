"use strict";

const request = require('request'),
      myUtils = require('./utils.js');

const SHOW_ABORTED = false;

const args = myUtils.getArgs(),
      START_LINK = args.rootUrl,
      REG_EXP = args.regExp,
      MAX_REQUESTS = args.maxRequests;

let nLinksAlreadyProcessed = 0,
    nLinksWithMatches = 0,
    averageMatches = 0,
    linksAlreadyRequested = new Set(),
    nLinksAborted = 0;

request(START_LINK, processLink);
linksAlreadyRequested.add(START_LINK);


// ------------------------ Functions declarations ----------------------------

function processLink(error, response, body) {

    if (error) {
        nLinksAborted++;
        if (SHOW_ABORTED) {
            myUtils.writeOverLastLine(`<<ERROR>> ${error}\n`);
            myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
        }

    } else if (response.statusCode != 200) {
        nLinksAborted++;
        if (SHOW_ABORTED) {
            myUtils.writeOverLastLine(`<<REJECTED>> (Code: ${response.statusCode}) ${response.request.uri.href}\n`);
            myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
        }

    } else if (!(response.headers['content-type']).startsWith('text/html')) {
        nLinksAborted++;
        if (SHOW_ABORTED) {
            myUtils.writeOverLastLine(`<<NOT_HTML/TEXT>> (Content type: ${response.headers['content-type']}) ${response.request.uri.href}\n`);
            myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
        }

    } else {
        const linksInThisPage = myUtils.getLinks(response);
        
        for (let i=0; i<linksInThisPage.length; i++) {
            if (linksAlreadyRequested.size == MAX_REQUESTS) {
                break;
            }
            let link = linksInThisPage[i];
            if (!linksAlreadyRequested.has(link)) {
                request(link, processLink);
                linksAlreadyRequested.add(link);
            }
        }
        
        const matches = myUtils.countRegexpMatches(response, REG_EXP);
        if (matches > 0) {
            averageMatches = myUtils.updateAverage(averageMatches, nLinksWithMatches, matches);
            nLinksWithMatches++;
            myUtils.writeOverLastLine(`[${matches}] ${response.request.uri.href}\n`);
        }

        nLinksAlreadyProcessed++;
        myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
    }
}
