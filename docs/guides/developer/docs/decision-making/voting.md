Voting
======

Occasionally a *feel* for consensus is not enough. Sometimes we need to have a measurable consensus. For example, when
voting new committers in or to approve a release.

Preparing for a Vote
--------------------

Before calling a vote it is important to ensure that the community is given time to discuss the upcoming vote. This will
be done by posting an email to the developers list indicating the intention to call a vote and the options available. By
the time a vote is called there should already be consensus in the community. The vote itself is, normally, a formality.

Calling a Vote
--------------

Once it is time to call the vote, a mail is posted to the committers list with a subject starting with `[#vote]`. This
enables the community members to ensure they do not miss an important vote thread. It also indicates that this is not
consensus building but a formal vote.

Casting Your Vote
-----------------

The notation used in voting is:

- `+1` Yes I agree
- `0` I have no strong opinion
- `-1` I object on the following grounds…

If you object you must support your objection and provide an alternative course of action that you are willing and able
to implement (where appropriate).

Votes should generally be permitted to run for at least 72 hours to provide an opportunity for all concerned persons to
participate regardless of their geographic locations.


Publishing Results
------------------

After the voting is done, the outcome should be published on the public developer list. A result may be kept private, if
deemed necessary, for votes on security-relevant or personal topics.

Binding Votes
-------------

In Opencast, only committers have binding votes. All others are either discouraged from voting (to keep the noise down)
or else have their votes considered in an advisory nature only.

When to Vote
------------

There are essentially three occasions to vote:

- Releases
- Changes to the Committer body
- Significant changes to the development process

Veto and Majority
-----------------

By default, all committers have a veto right when voting, meaning that a `-1` will effectively stop whatever was voted
for. After addressing the issue, a second vote may be called for. Depending on the discussion, at this point, the
initiator may determine this to be a majority vote.

In the rare case that a majority vote among committers is called for, the vote is a simple majority vote among
participating committers without regards to any form of quorum. The vote will pass if there were more positive (`+1`)
votes then negative ones (`-1`), implying that at least one committer needs to respond.


Formal Votes for Code Changes
-----------------------------

Usually, code changes have consensus and there should be no need for voting. If in doubt, contributors can [propose
changes](consensus-building.md) on list in advance. Additionally, consensus may be reached though discussion as part of
the review process.

There might be the rare case of a dispute between reviewer(s) and contributor(s) during the review process which cannot
be resolved easily. In such a case, both parties can call a formal majority vote to settle the issue.
