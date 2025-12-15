/*
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

import { Events, EventLogPlugin, createElementWithHtmlText } from 'paella-core';
import '../css/QuizPlugin.css';

export default class QuizPlugin extends EventLogPlugin {

  // Initialize
  async load() {
    this._quiz = null; // DOM element
    this._quizJSON = []; // Infos from the Opencast mediapackage

    /* Demo json for quick developing purposes */
    const myTestJson = [
      {
        start: 2000,
        type: 'multipleChoice',
        question: 'Which is a fruit?',
        answers: [
          {
            text: 'Banana',
            correct: true
          },
          {
            text: 'Cucumber',
            correct: false
          },
          {
            text: 'Tomato',
            correct: true
          },
          {
            text: 'Adjustable side table, walnut',
            correct: false
          }
        ],
      },
    ];
    this._quizJSON = myTestJson;
  }

  // Define which events we subscribe too
  get events() {
    return [
      Events.PLAYER_LOADED,
      Events.TIMEUPDATE,
      Events.PLAY,
      Events.SEEK
    ];
  }

  async onEvent(event, params) {
    // Load info from Opencast
    if (event === Events.PLAYER_LOADED) {
      // TODO: Handle multiple quiz files
      const quiz = this.player?._videoManifest?.quizzes?.[0];
      this._quizJSON = await this.loadQuizzesFromOpencast(quiz.url);
    }

    // Hide Quiz if user is trying to play the video again
    if (this._quiz && (event === Events.PLAY || event === Events.SEEK)) {
      this.removeQuiz();
    }

    // Display/Hide quiz
    this._quizJSON.forEach((info, index) => {
      if (info.shown === undefined) {
        info.shown = false;
      }

      // TODO: Avoid quizzes overlapping
      const start = (info.start / 1000);
      if (!info.shown && params.currentTime >= start && !this._quiz) {
        this.player.pause();
        this.createQuiz(info, index);
        info.shown = true;
      }

      if (params.currentTime < start) {
        info.shown = false; // Reset flag
      }
    });

    // // Old code for manually fixing fullscreen issue
    // // Requires Events.FULLSCREEN_CHANGED in the get events() method
    // if (event === Events.FULLSCREEN_CHANGED) {
    //   const fsElement = document.fullscreenElement;
    //   const quiz = document.querySelector('.quiz-plugin-root');
    //   if (fsElement && quiz && !fsElement.contains(quiz)) {
    //     fsElement.appendChild(quiz);
    //   }
    // }

  }

  // Add a textbox to the DOM
  createQuiz(info) {
    if (!this._quiz) {
      /* HTML */
      const quiz = createElementWithHtmlText('<div class="quiz-plugin-root"> </div>');

      const quizLeft = createElementWithHtmlText(`
        <div class="quiz-plugin-left"> </div>
      `, quiz);

      createElementWithHtmlText(`
        <div class="quiz-plugin-question">${info.question}</div>
      `, quizLeft);

      const answers = createElementWithHtmlText(`
        <div class="quiz-plugin-answers"> </div>
      `, quizLeft);

      info.answers?.map((answer) => {
        createElementWithHtmlText(`
          <label><input type="checkbox" value="${answer.text}">${answer.text}</label>
        `, answers);
      });

      const bottomButtons = createElementWithHtmlText(`
        <div class="quiz-bottom-buttons"></div>
      `, quizLeft);

      // eslint-disable-next-line no-unused-vars
      const skipButton = createElementWithHtmlText(`
        <button class="quiz-submit-button">Skip</button>
      `, bottomButtons);

      const submit = createElementWithHtmlText(`
        <button class="quiz-submit-button">Submit</button>
      `, bottomButtons);

      const quizRight = createElementWithHtmlText(`
        <div class="quiz-plugin-right"> </div>
      `, quiz);

      createElementWithHtmlText(`
        <div></div>
      `, quizRight);

      const result = createElementWithHtmlText(`
        <div class="quiz-plugin-result"></div>
      `, quizRight);

      const continueButton = createElementWithHtmlText(`
        <button class="quiz-submit-button">Continue</button>
      `, quizRight);
      continueButton.style.display = 'none';

      /* Script */
      const correctAnswers = info.answers
        .filter(answer => answer.correct)
        .map(answer => answer.text);

      submit.addEventListener('click', () => {
        const checkboxes = answers.querySelectorAll('.quiz-plugin-answers input[type="checkbox"]');
        const selected = [];
        let correctCount = 0;

        // Reset styles
        checkboxes.forEach(checkbox => {
          const label = checkbox.parentElement;
          label.classList.remove('quiz-plugin-correct');
        });

        // Evaluate answers
        checkboxes.forEach(box => {
          const label = box.parentElement;
          const isChecked = box.checked;
          const isCorrect = correctAnswers.includes(box.value);

          if (isChecked) {
            selected.push(box.value);
            if (isCorrect) {
              label.classList.add('quiz-plugin-correct');
              correctCount++;
            }
          }
        });

        // Build result message
        const totalCorrect = correctAnswers.length;
        let resultMsg = `You got ${correctCount} out of ${totalCorrect} correct.`;

        // Show missed answers
        const missed = correctAnswers.filter(answer => !selected.includes(answer));
        if (missed.length > 0) {
          const names = missed.map(a => a[0].toUpperCase() + a.slice(1)).join(', ');
          resultMsg += `<br><span class="quiz-plugin-correct-list">Correct answers you missed: ${names}</span>`;
        }

        result.innerHTML = resultMsg;
        continueButton.style.display = 'block';
      });

      skipButton.addEventListener('click', () => {
        this.removeQuiz();
      });

      continueButton.addEventListener('click', () => {
        this.removeQuiz();
      });


      this._quiz = quiz;
      // Append to player-container to not dissappear during fullscreen
      document.querySelector('.player-container').appendChild(this._quiz);
    }
  }

  // Remove a textbox from the DOM
  removeQuiz() {
    if (this._quiz) {
      document.querySelector('.player-container').removeChild(this._quiz);
      this._quiz = null;
      this.player.play();
    }
  }

  // Query Opencast for a json with info about textboxes
  loadQuizzesFromOpencast = async (url) => {
    let quizzes = [];
    const response = await fetch(url);
    if (response.ok) {
      try {
        quizzes = response.json();
      }
      catch (e) {
        this.player.log.warn('Error loading quizzes');
      }
    }
    return quizzes;
  };
}
