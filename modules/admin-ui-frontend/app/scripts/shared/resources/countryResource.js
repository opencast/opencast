/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
'use strict';

// TODO: This is a Service not a $resource, but should be implemented as one
// TODO: 1. Implement a CountryEndpoint 2. Make this a $resource
angular.module('adminNg.resources')
.factory('CountryResource', function () {

  var countries = {};

  countries.getCountries = function() {
    return [
      {
        'code': 'AF',
        'name': 'Afghanistan'
      },
      {
        'code': 'AL',
        'name': 'Albania'
      },
      {
        'code': 'DE',
        'name': 'Germany'
      },
      {
        'code': 'AD',
        'name': 'Andorra'
      },
      {
        'code': 'AO',
        'name': 'Angola'
      },
      {
        'code': 'AI',
        'name': 'Anguilla'
      },
      {
        'code': 'AG',
        'name': 'Antigua and Barbuda'
      },
      {
        'code': 'AQ',
        'name': 'Antarctica'
      },
      {
        'code': 'SA',
        'name': 'Saudi Arabia'
      },
      {
        'code': 'DZ',
        'name': 'Algeria'
      },
      {
        'code': 'AR',
        'name': 'Argentina'
      },
      {
        'code': 'AM',
        'name': 'Armenia'
      },
      {
        'code': 'AW',
        'name': 'Aruba'
      },
      {
        'code': 'AU',
        'name': 'Australia'
      },
      {
        'code': 'AT',
        'name': 'Austria'
      },
      {
        'code': 'AZ',
        'name': 'Azerbaijan'
      },
      {
        'code': 'BS',
        'name': 'Bahamas'
      },
      {
        'code': 'BH',
        'name': 'Bahrain'
      },
      {
        'code': 'BD',
        'name': 'Bangladesh'
      },
      {
        'code': 'BB',
        'name': 'Barbados'
      },
      {
        'code': 'BZ',
        'name': 'Belize'
      },
      {
        'code': 'BJ',
        'name': 'Benin'
      },
      {
        'code': 'BM',
        'name': 'Bermuda'
      },
      {
        'code': 'BY',
        'name': 'Belarus'
      },
      {
        'code': 'BO',
        'name': 'Bolivia'
      },
      {
        'code': 'BQ',
        'name': 'Bonaire'
      },
      {
        'code': 'BA',
        'name': 'Bosnia and Herzegovina'
      },
      {
        'code': 'BW',
        'name': 'Botswana'
      },
      {
        'code': 'BR',
        'name': 'Brazil'
      },
      {
        'code': 'BN',
        'name': 'Brunei'
      },
      {
        'code': 'BG',
        'name': 'Bulgaria'
      },
      {
        'code': 'BF',
        'name': 'Burkina Faso'
      },
      {
        'code': 'BI',
        'name': 'Burundi'
      },
      {
        'code': 'BT',
        'name': 'Bhutan'
      },
      {
        'code': 'BE',
        'name': 'Belgium'
      },
      {
        'code': 'CV',
        'name': 'Cabo Verde'
      },
      {
        'code': 'KH',
        'name': 'Cambodia'
      },
      {
        'code': 'CM',
        'name': 'Cameroon'
      },
      {
        'code': 'CA',
        'name': 'Canada'
      },
      {
        'code': 'TD',
        'name': 'Chad'
      },
      {
        'code': 'CL',
        'name': 'Chile'
      },
      {
        'code': 'CN',
        'name': 'China'
      },
      {
        'code': 'CY',
        'name': 'Cyprus'
      },
      {
        'code': 'VA',
        'name': 'Vatican City'
      },
      {
        'code': 'CO',
        'name': 'Colombia'
      },
      {
        'code': 'KM',
        'name': 'Comoros'
      },
      {
        'code': 'CG',
        'name': 'Republic of the Congo'
      },
      {
        'code': 'KP',
        'name': 'North Korea'
      },
      {
        'code': 'KR',
        'name': 'South Korea'
      },
      {
        'code': 'CR',
        'name': 'Costa Rica'
      },
      {
        'code': 'CI',
        'name': 'Ivory Coast'
      },
      {
        'code': 'HR',
        'name': 'Croatia'
      },
      {
        'code': 'CU',
        'name': 'Cuba'
      },
      {
        'code': 'CW',
        'name': 'Curacao'
      },
      {
        'code': 'DK',
        'name': 'Denmark'
      },
      {
        'code': 'DM',
        'name': 'Dominica'
      },
      {
        'code': 'EC',
        'name': 'Ecuador'
      },
      {
        'code': 'EG',
        'name': 'Egypt'
      },
      {
        'code': 'SV',
        'name': 'El Salvador'
      },
      {
        'code': 'AE',
        'name': 'United Arab Emirates'
      },
      {
        'code': 'ER',
        'name': 'Eritrea'
      },
      {
        'code': 'SK',
        'name': 'Slovakia'
      },
      {
        'code': 'SI',
        'name': 'Slovenia'
      },
      {
        'code': 'ES',
        'name': 'Spain'
      },
      {
        'code': 'US',
        'name': 'United States'
      },
      {
        'code': 'EE',
        'name': 'Estonia'
      },
      {
        'code': 'ET',
        'name': 'Ethiopia'
      },
      {
        'code': 'PH',
        'name': 'Philippines'
      },
      {
        'code': 'FI',
        'name': 'Finland'
      },
      {
        'code': 'FJ',
        'name': 'Fiji'
      },
      {
        'code': 'FR',
        'name': 'France'
      },
      {
        'code': 'GA',
        'name': 'Gabon'
      },
      {
        'code': 'GM',
        'name': 'Gambia'
      },
      {
        'code': 'GE',
        'name': 'Georgia'
      },
      {
        'code': 'GH',
        'name': 'Ghana'
      },
      {
        'code': 'GI',
        'name': 'Gibraltar'
      },
      {
        'code': 'GD',
        'name': 'Grenada'
      },
      {
        'code': 'GR',
        'name': 'Greece'
      },
      {
        'code': 'GL',
        'name': 'Greenland'
      },
      {
        'code': 'GP',
        'name': 'Guadeloupe'
      },
      {
        'code': 'GU',
        'name': 'Guam'
      },
      {
        'code': 'GT',
        'name': 'Guatemala'
      },
      {
        'code': 'GF',
        'name': 'French Guiana'
      },
      {
        'code': 'GG',
        'name': 'Guernsey'
      },
      {
        'code': 'GN',
        'name': 'Guinea'
      },
      {
        'code': 'GQ',
        'name': 'Equatorial Guinea'
      },
      {
        'code': 'GW',
        'name': 'Guinea-Bissau'
      },
      {
        'code': 'GY',
        'name': 'Guyana'
      },
      {
        'code': 'HT',
        'name': 'Haiti'
      },
      {
        'code': 'NL',
        'name': 'Netherlands'
      },
      {
        'code': 'HN',
        'name': 'Honduras'
      },
      {
        'code': 'HK',
        'name': 'Hong Kong'
      },
      {
        'code': 'HU',
        'name': 'Hungary'
      },
      {
        'code': 'IN',
        'name': 'India'
      },
      {
        'code': 'ID',
        'name': 'Indonesia'
      },
      {
        'code': 'IQ',
        'name': 'Iraq'
      },
      {
        'code': 'IE',
        'name': 'Ireland'
      },
      {
        'code': 'IR',
        'name': 'Iran'
      },
      {
        'code': 'BV',
        'name': 'Bouvet Island'
      },
      {
        'code': 'CX',
        'name': 'Christmas Island'
      },
      {
        'code': 'NU',
        'name': 'Niue'
      },
      {
        'code': 'NF',
        'name': 'Norfolk Island'
      },
      {
        'code': 'IM',
        'name': 'Isle of Man'
      },
      {
        'code': 'IS',
        'name': 'Iceland'
      },
      {
        'code': 'KY',
        'name': 'Cayman Islands'
      },
      {
        'code': 'CC',
        'name': 'Cocos [Keeling] Islands'
      },
      {
        'code': 'CK',
        'name': 'Cook Islands'
      },
      {
        'code': 'FO',
        'name': 'Faroe Islands'
      },
      {
        'code': 'GS',
        'name': 'South Georgia and the South Sandwich Islands'
      },
      {
        'code': 'HM',
        'name': 'Heard Island and McDonald Islands'
      },
      {
        'code': 'FK',
        'name': 'Falkland Islands'
      },
      {
        'code': 'MP',
        'name': 'Northern Mariana Islands'
      },
      {
        'code': 'MH',
        'name': 'Marshall Islands'
      },
      {
        'code': 'PN',
        'name': 'Pitcairn Islands'
      },
      {
        'code': 'SB',
        'name': 'Solomon Islands'
      },
      {
        'code': 'TC',
        'name': 'Turks and Caicos Islands'
      },
      {
        'code': 'VG',
        'name': 'British Virgin Islands'
      },
      {
        'code': 'VI',
        'name': 'U.S. Virgin Islands'
      },
      {
        'code': 'AX',
        'name': 'Åland'
      },
      {
        'code': 'UM',
        'name': 'U.S. Minor Outlying Islands'
      },
      {
        'code': 'IL',
        'name': 'Israel'
      },
      {
        'code': 'IT',
        'name': 'Italy'
      },
      {
        'code': 'JM',
        'name': 'Jamaica'
      },
      {
        'code': 'JP',
        'name': 'Japan'
      },
      {
        'code': 'JE',
        'name': 'Jersey'
      },
      {
        'code': 'JO',
        'name': 'Jordan'
      },
      {
        'code': 'KZ',
        'name': 'Kazakhstan'
      },
      {
        'code': 'KE',
        'name': 'Kenya'
      },
      {
        'code': 'KG',
        'name': 'Kyrgyzstan'
      },
      {
        'code': 'KI',
        'name': 'Kiribati'
      },
      {
        'code': 'XK',
        'name': 'Kosovo'
      },
      {
        'code': 'KW',
        'name': 'Kuwait'
      },
      {
        'code': 'LA',
        'name': 'Laos'
      },
      {
        'code': 'LS',
        'name': 'Lesotho'
      },
      {
        'code': 'LV',
        'name': 'Latvia'
      },
      {
        'code': 'LR',
        'name': 'Liberia'
      },
      {
        'code': 'LY',
        'name': 'Libya'
      },
      {
        'code': 'LI',
        'name': 'Liechtenstein'
      },
      {
        'code': 'LT',
        'name': 'Lithuania'
      },
      {
        'code': 'LU',
        'name': 'Luxembourg'
      },
      {
        'code': 'LB',
        'name': 'Lebanon'
      },
      {
        'code': 'MO',
        'name': 'Macao'
      },
      {
        'code': 'MK',
        'name': 'North Macedonia'
      },
      {
        'code': 'MG',
        'name': 'Madagascar'
      },
      {
        'code': 'MY',
        'name': 'Malaysia'
      },
      {
        'code': 'MW',
        'name': 'Malawi'
      },
      {
        'code': 'MV',
        'name': 'Maldives'
      },
      {
        'code': 'ML',
        'name': 'Mali'
      },
      {
        'code': 'MT',
        'name': 'Malta'
      },
      {
        'code': 'MA',
        'name': 'Morocco'
      },
      {
        'code': 'MQ',
        'name': 'Martinique'
      },
      {
        'code': 'MU',
        'name': 'Mauritius'
      },
      {
        'code': 'MR',
        'name': 'Mauritania'
      },
      {
        'code': 'YT',
        'name': 'Mayotte'
      },
      {
        'code': 'FM',
        'name': 'Micronesia'
      },
      {
        'code': 'MD',
        'name': 'Moldova'
      },
      {
        'code': 'MN',
        'name': 'Mongolia'
      },
      {
        'code': 'ME',
        'name': 'Montenegro'
      },
      {
        'code': 'MS',
        'name': 'Montserrat'
      },
      {
        'code': 'MZ',
        'name': 'Mozambique'
      },
      {
        'code': 'MM',
        'name': 'Myanmar [Burma]'
      },
      {
        'code': 'MX',
        'name': 'Mexico'
      },
      {
        'code': 'MC',
        'name': 'Monaco'
      },
      {
        'code': 'NA',
        'name': 'Namibia'
      },
      {
        'code': 'NR',
        'name': 'Nauru'
      },
      {
        'code': 'NP',
        'name': 'Nepal'
      },
      {
        'code': 'NI',
        'name': 'Nicaragua'
      },
      {
        'code': 'NG',
        'name': 'Nigeria'
      },
      {
        'code': 'NO',
        'name': 'Norway'
      },
      {
        'code': 'NC',
        'name': 'New Caledonia'
      },
      {
        'code': 'NZ',
        'name': 'New Zealand'
      },
      {
        'code': 'NE',
        'name': 'Niger'
      },
      {
        'code': 'OM',
        'name': 'Oman'
      },
      {
        'code': 'PK',
        'name': 'Pakistan'
      },
      {
        'code': 'PW',
        'name': 'Palau'
      },
      {
        'code': 'PA',
        'name': 'Panama'
      },
      {
        'code': 'PG',
        'name': 'Papua New Guinea'
      },
      {
        'code': 'PY',
        'name': 'Paraguay'
      },
      {
        'code': 'PE',
        'name': 'Peru'
      },
      {
        'code': 'PF',
        'name': 'French Polynesia'
      },
      {
        'code': 'PL',
        'name': 'Poland'
      },
      {
        'code': 'PT',
        'name': 'Portugal'
      },
      {
        'code': 'PR',
        'name': 'Puerto Rico'
      },
      {
        'code': 'QA',
        'name': 'Qatar'
      },
      {
        'code': 'GB',
        'name': 'United Kingdom'
      },
      {
        'code': 'CF',
        'name': 'Central African Republic'
      },
      {
        'code': 'CZ',
        'name': 'Czechia'
      },
      {
        'code': 'CD',
        'name': 'Democratic Republic of the Congo'
      },
      {
        'code': 'DO',
        'name': 'Dominican Republic'
      },
      {
        'code': 'RE',
        'name': 'Réunion'
      },
      {
        'code': 'RW',
        'name': 'Rwanda'
      },
      {
        'code': 'RO',
        'name': 'Romania'
      },
      {
        'code': 'RU',
        'name': 'Russia'
      },
      {
        'code': 'WS',
        'name': 'Samoa'
      },
      {
        'code': 'AS',
        'name': 'American Samoa'
      },
      {
        'code': 'BL',
        'name': 'Saint Barthélemy'
      },
      {
        'code': 'KN',
        'name': 'Saint Kitts and Nevis'
      },
      {
        'code': 'SM',
        'name': 'San Marino'
      },
      {
        'code': 'MF',
        'name': 'Saint Martin'
      },
      {
        'code': 'SX',
        'name': 'Sint Maarten'
      },
      {
        'code': 'PM',
        'name': 'Saint Pierre and Miquelon'
      },
      {
        'code': 'VC',
        'name': 'Saint Vincent and the Grenadines'
      },
      {
        'code': 'SH',
        'name': 'Saint Helena'
      },
      {
        'code': 'LC',
        'name': 'Saint Lucia'
      },
      {
        'code': 'ST',
        'name': 'São Tomé and Príncipe'
      },
      {
        'code': 'SN',
        'name': 'Senegal'
      },
      {
        'code': 'RS',
        'name': 'Serbia'
      },
      {
        'code': 'SC',
        'name': 'Seychelles'
      },
      {
        'code': 'SL',
        'name': 'Sierra Leone'
      },
      {
        'code': 'SG',
        'name': 'Singapore'
      },
      {
        'code': 'SY',
        'name': 'Syria'
      },
      {
        'code': 'SO',
        'name': 'Somalia'
      },
      {
        'code': 'LK',
        'name': 'Sri Lanka'
      },
      {
        'code': 'SZ',
        'name': 'Eswatini'
      },
      {
        'code': 'ZA',
        'name': 'South Africa'
      },
      {
        'code': 'SD',
        'name': 'Sudan'
      },
      {
        'code': 'SS',
        'name': 'South Sudan'
      },
      {
        'code': 'SE',
        'name': 'Sweden'
      },
      {
        'code': 'CH',
        'name': 'Switzerland'
      },
      {
        'code': 'SR',
        'name': 'Suriname'
      },
      {
        'code': 'SJ',
        'name': 'Svalbard and Jan Mayen'
      },
      {
        'code': 'EH',
        'name': 'Western Sahara'
      },
      {
        'code': 'TH',
        'name': 'Thailand'
      },
      {
        'code': 'TW',
        'name': 'Taiwan'
      },
      {
        'code': 'TZ',
        'name': 'Tanzania'
      },
      {
        'code': 'TJ',
        'name': 'Tajikistan'
      },
      {
        'code': 'IO',
        'name': 'British Indian Ocean Territory'
      },
      {
        'code': 'TF',
        'name': 'French Southern Territories'
      },
      {
        'code': 'PS',
        'name': 'Palestine'
      },
      {
        'code': 'TL',
        'name': 'Timor-Leste'
      },
      {
        'code': 'TG',
        'name': 'Togo'
      },
      {
        'code': 'TK',
        'name': 'Tokelau'
      },
      {
        'code': 'TO',
        'name': 'Tonga'
      },
      {
        'code': 'TT',
        'name': 'Trinidad and Tobago'
      },
      {
        'code': 'TM',
        'name': 'Turkmenistan'
      },
      {
        'code': 'TR',
        'name': 'Turkey'
      },
      {
        'code': 'TV',
        'name': 'Tuvalu'
      },
      {
        'code': 'TN',
        'name': 'Tunisia'
      },
      {
        'code': 'UA',
        'name': 'Ukraine'
      },
      {
        'code': 'UG',
        'name': 'Uganda'
      },
      {
        'code': 'UY',
        'name': 'Uruguay'
      },
      {
        'code': 'UZ',
        'name': 'Uzbekistan'
      },
      {
        'code': 'VU',
        'name': 'Vanuatu'
      },
      {
        'code': 'VE',
        'name': 'Venezuela'
      },
      {
        'code': 'VN',
        'name': 'Vietnam'
      },
      {
        'code': 'WF',
        'name': 'Wallis and Futuna'
      },
      {
        'code': 'YE',
        'name': 'Yemen'
      },
      {
        'code': 'DJ',
        'name': 'Djibouti'
      },
      {
        'code': 'ZM',
        'name': 'Zambia'
      },
      {
        'code': 'ZW',
        'name': 'Zimbabwe'
      }
    ];
  };

  return countries;
});
