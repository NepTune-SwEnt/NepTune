import { initializeTestEnvironment, type RulesTestEnvironment } from '@firebase/rules-unit-testing';
import { readFile } from 'fs/promises';
import { myToDo, notMyToDo } from './test-data.js';
import { setLogLevel } from 'firebase/firestore';

const HOST = "127.0.0.1";
const FIRESTORE_PORT = 8080;

export async function checkIfEmulatorsAreRunning() {
  try {
    await fetch(`http://${HOST}:${FIRESTORE_PORT}`);
  } catch (e) {
    throw new Error(
      "Emulators are not running, please start them before running tests."
    );
  }
}

export async function setupEnvironment(): Promise<RulesTestEnvironment> {
  process.env["FIRESTORE_EMULATOR_HOST"] = `${HOST}:${FIRESTORE_PORT}`;

  return await initializeTestEnvironment({
    projectId: 'demo-unit-tests',
    firestore: {
      rules: await readFile("../firestore.rules", "utf-8"),
      host: HOST,
      port: FIRESTORE_PORT,
    },
  });
}

export async function setupFirestore(env: RulesTestEnvironment) {
  // Removes warnings when permission is denied in tests
  setLogLevel('error');

  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    const collection = ctx.firestore().collection('todos');

    await collection.doc(myToDo.uid).set(myToDo);
    await collection.doc(notMyToDo.uid).set(notMyToDo);
  });
}

export async function setup() {
  /** Ensure emulators are running **/
  await checkIfEmulatorsAreRunning();

  /** Initialize testing environment **/
  return await setupEnvironment();
}
